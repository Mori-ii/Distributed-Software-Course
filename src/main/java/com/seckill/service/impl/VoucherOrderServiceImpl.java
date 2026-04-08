package com.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.config.KafkaConfig;
import com.seckill.dto.Result;
import com.seckill.entity.VoucherOrder;
import com.seckill.mapper.VoucherOrderMapper;
import com.seckill.service.ISeckillVoucherService;
import com.seckill.service.IVoucherOrderService;
import com.seckill.utils.SnowflakeIdWorker;
import com.seckill.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 秒杀下单服务实现
 * 流程：Redis Lua 原子检查库存+幂等 → Kafka 异步削峰 → 消费者落库
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private SnowflakeIdWorker snowflakeIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private KafkaTemplate<String, VoucherOrder> kafkaTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private IVoucherOrderService proxy;

    @PostConstruct
    private void init() {
        proxy = (IVoucherOrderService) AopContext.currentProxy();
    }

    /**
     * 秒杀下单入口：
     * 1. Lua 脚本原子判断库存 + 幂等（同一用户同一券只能下一单）
     * 2. 通过 Kafka 异步发送订单消息，实现削峰填谷
     * 3. 立即返回订单 ID，消费者异步落库
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = snowflakeIdWorker.nextId();

        // 执行 Lua 脚本：检查库存、幂等、扣减 Redis 库存、记录用户已购
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );

        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        // 构建订单对象发送到 Kafka
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        kafkaTemplate.send(KafkaConfig.SECKILL_ORDER_TOPIC, String.valueOf(userId), voucherOrder);

        return Result.ok(orderId);
    }

    /**
     * Kafka 消费者：异步处理订单落库
     * 使用 Redisson 分布式锁保证同一用户并发安全
     */
    @KafkaListener(topics = KafkaConfig.SECKILL_ORDER_TOPIC, groupId = "seckill-order-group")
    public void onSeckillOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLocked = lock.tryLock();
        if (!isLocked) {
            log.error("不允许重复下单，userId={}", userId);
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 事务方法：DB 扣库存 + 保存订单
     * 双重幂等校验：Redis 已在 Lua 脚本中拦截，此处再做 DB 兜底
     */
    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        // DB 兜底幂等：防止极端情况下重复消费
        long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            log.warn("重复下单被拦截，userId={}, voucherId={}", userId, voucherId);
            return;
        }

        // 扣减 DB 库存（乐观锁：stock > 0）
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足，voucherId={}", voucherId);
            return;
        }

        save(voucherOrder);
    }

    /**
     * 按订单 ID 查询订单
     */
    @Override
    public Result queryOrderById(Long orderId) {
        VoucherOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.ok(order);
    }

    /**
     * 按用户 ID 查询该用户所有订单
     */
    @Override
    public Result queryOrdersByUserId(Long userId) {
        List<VoucherOrder> orders = query().eq("user_id", userId).list();
        return Result.ok(orders);
    }
}
