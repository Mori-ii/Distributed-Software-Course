package cn.edu.distcourse.flashsale.service;

import cn.edu.distcourse.flashsale.bootstrap.InventoryWarmUpTask;
import cn.edu.distcourse.flashsale.dao.*;
import cn.edu.distcourse.flashsale.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀核心业务逻辑
 * 实现 Redis 预扣减 + 数据库乐观锁 + 唯一索引 三重防护机制
 */
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
     * 执行秒杀抢购
     * 流程：Redis 防重检查 → Redis 库存预扣减 → 数据库乐观锁扣减 → 创建订单
     *
     * @param memberId 用户ID
     * @param flashId  秒杀活动ID
     * @return 成功创建的订单
     */
    @Transactional
    public TradeOrder snap(Long memberId, Long flashId) {

        // === 快速路径：Redis 缓存层检查 ===

        // 第一步：检查是否已抢购过（防重复）
        String boughtKey = InventoryWarmUpTask.BOUGHT_KEY + flashId;
        if (Boolean.TRUE.equals(redis.opsForSet().isMember(boughtKey, String.valueOf(memberId)))) {
            throw new RuntimeException("您已经抢购过该商品，不能重复购买");
        }

        // 第二步：Redis 原子预扣库存
        String invKey = InventoryWarmUpTask.INV_KEY + flashId;
        Long left = redis.opsForValue().decrement(invKey);
        if (left == null || left < 0) {
            // 库存不足，回滚 Redis 扣减
            redis.opsForValue().increment(invKey);
            throw new RuntimeException("商品已售罄");
        }

        // === 慢速路径：数据库操作 ===

        try {
            FlashItem fi = flashItemMapper.findById(flashId);
            if (fi == null) {
                throw new RuntimeException("秒杀活动不存在");
            }

            // 数据库层防重检查
            FlashGuard existing = flashGuardMapper.findByMemberAndProduct(memberId, fi.getProductId());
            if (existing != null) {
                throw new RuntimeException("您已经抢购过该商品，不能重复购买");
            }

            // 乐观锁扣减库存（CAS: remaining > 0 AND version = ?）
            int rows = flashItemMapper.deductStock(flashId, fi.getVersion());
            if (rows == 0) {
                throw new RuntimeException("库存冲突或已售罄，请重试");
            }

            Product product = productMapper.findById(fi.getProductId());

            // 创建交易订单
            TradeOrder order = new TradeOrder();
            order.setMemberId(memberId);
            order.setSellerId(product.getSellerId());
            order.setProductId(product.getId());
            order.setProductName(product.getProductName());
            order.setAmount(fi.getFlashPrice());
            order.setStatus(0);  // 待付款
            tradeOrderMapper.save(order);

            // 创建防重购记录（唯一索引兜底）
            FlashGuard guard = new FlashGuard();
            guard.setMemberId(memberId);
            guard.setOrderId(order.getId());
            guard.setProductId(product.getId());
            flashGuardMapper.save(guard);

            // 在 Redis 中标记该用户已购买
            redis.opsForSet().add(boughtKey, String.valueOf(memberId));

            return order;
        } catch (RuntimeException ex) {
            // 出现异常时回滚 Redis 预扣的库存
            redis.opsForValue().increment(invKey);
            throw ex;
        }
    }
}
