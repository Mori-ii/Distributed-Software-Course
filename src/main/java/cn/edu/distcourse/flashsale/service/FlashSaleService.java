package cn.edu.distcourse.flashsale.service;

import cn.edu.distcourse.flashsale.bootstrap.InventoryWarmUpTask;
import cn.edu.distcourse.flashsale.dao.*;
import cn.edu.distcourse.flashsale.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashItemMapper flashItemMapper;
    private final ProductMapper productMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final FlashGuardMapper flashGuardMapper;
    private final MemberMapper memberMapper;
    private final StringRedisTemplate redis;

    /**
     * Attempt to snap (purchase) a flash item.
     * Flow: Redis duplicate-check -> Redis stock pre-deduct -> DB optimistic-lock deduct -> create order.
     */
    @Transactional
    public TradeOrder snap(Long memberId, Long flashId) {

        // --- fast path: Redis checks ---

        String boughtKey = InventoryWarmUpTask.BOUGHT_KEY + flashId;
        if (Boolean.TRUE.equals(redis.opsForSet().isMember(boughtKey, String.valueOf(memberId)))) {
            throw new RuntimeException("Already purchased this item");
        }

        String invKey = InventoryWarmUpTask.INV_KEY + flashId;
        Long left = redis.opsForValue().decrement(invKey);
        if (left == null || left < 0) {
            redis.opsForValue().increment(invKey);
            throw new RuntimeException("Sold out");
        }

        // --- slow path: database operations ---

        try {
            FlashItem fi = flashItemMapper.findById(flashId);
            if (fi == null) {
                throw new RuntimeException("Flash item not found");
            }

            // DB-level duplicate guard
            FlashGuard existing = flashGuardMapper.findByMemberAndProduct(memberId, fi.getProductId());
            if (existing != null) {
                throw new RuntimeException("Already purchased this item");
            }

            // Optimistic-lock stock deduction
            int rows = flashItemMapper.deductStock(flashId, fi.getVersion());
            if (rows == 0) {
                throw new RuntimeException("Stock conflict or sold out");
            }

            Product product = productMapper.findById(fi.getProductId());

            // Create trade order
            TradeOrder order = new TradeOrder();
            order.setMemberId(memberId);
            order.setSellerId(product.getSellerId());
            order.setProductId(product.getId());
            order.setProductName(product.getProductName());
            order.setAmount(fi.getFlashPrice());
            order.setStatus(0);
            tradeOrderMapper.save(order);

            // Create idempotency guard record
            FlashGuard guard = new FlashGuard();
            guard.setMemberId(memberId);
            guard.setOrderId(order.getId());
            guard.setProductId(product.getId());
            flashGuardMapper.save(guard);

            // Mark as purchased in Redis
            redis.opsForSet().add(boughtKey, String.valueOf(memberId));

            return order;
        } catch (RuntimeException ex) {
            redis.opsForValue().increment(invKey);
            throw ex;
        }
    }
}
