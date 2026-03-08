package cn.edu.distcourse.flashsale;

import cn.edu.distcourse.flashsale.dao.*;
import cn.edu.distcourse.flashsale.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据访问层集成测试
 * 标记 @Transactional 的测试方法会在执行后自动回滚，不影响数据库数据
 */
@SpringBootTest
class DaoLayerTest {

    @Autowired private MemberMapper memberMapper;
    @Autowired private SellerMapper sellerMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private FlashItemMapper flashItemMapper;
    @Autowired private TradeOrderMapper tradeOrderMapper;
    @Autowired private FlashGuardMapper flashGuardMapper;

    // ---------- 用户表测试 ----------

    @Test
    void 根据ID查询用户() {
        Member m = memberMapper.findById(1001L);
        System.out.println("[测试] 根据ID查询用户 => " + m);
        assertNotNull(m, "ID为1001的用户应存在");
        assertEquals("TestUser_Alpha", m.getAlias());
    }

    @Test
    @Transactional
    void 新增用户() {
        Member m = new Member();
        m.setAlias("测试用户");
        m.setPwd("e10adc3949ba59abbe56e057f20f883e");
        m.setSalt("abcdef");

        int cnt = memberMapper.save(m);
        System.out.println("[测试] 新增用户 => 影响行数=" + cnt + ", 分配ID=" + m.getId());
        assertEquals(1, cnt);
        assertNotNull(m.getId());
    }

    // ---------- 商家表测试 ----------

    @Test
    void 根据ID查询商家() {
        Seller s = sellerMapper.findById(1L);
        System.out.println("[测试] 根据ID查询商家 => " + s);
        assertNotNull(s, "ID为1的商家应存在");
    }

    @Test
    void 查询启用状态的商家() {
        List<Seller> list = sellerMapper.findAllActive();
        System.out.println("[测试] 启用状态的商家 => 数量=" + list.size());
        list.forEach(System.out::println);
        assertFalse(list.isEmpty());
    }

    // ---------- 商品表测试 ----------

    @Test
    void 根据ID查询商品() {
        Product p = productMapper.findById(1L);
        System.out.println("[测试] 根据ID查询商品 => " + p);
        assertNotNull(p, "ID为1的商品应存在");
        assertEquals(1L, p.getSellerId());
        System.out.println("  商品名=" + p.getProductName() + ", 价格=" + p.getPrice() + ", 库存=" + p.getStock());
    }

    @Test
    void 根据商家ID查询商品列表() {
        List<Product> list = productMapper.findBySellerId(1L);
        System.out.println("[测试] 商家1的商品列表 => 数量=" + list.size());
        list.forEach(System.out::println);
        assertFalse(list.isEmpty());
    }

    @Test
    @Transactional
    void 新增商品() {
        Product p = new Product();
        p.setSellerId(1L);
        p.setProductName("测试马克杯");
        p.setImageUrl("https://example.com/mug.png");
        p.setPrice(new BigDecimal("49.90"));
        p.setStock(100);

        int cnt = productMapper.save(p);
        System.out.println("[测试] 新增商品 => 影响行数=" + cnt + ", 分配ID=" + p.getId());
        assertEquals(1, cnt);
        assertNotNull(p.getId());
    }

    // ---------- 秒杀配置表测试（核心） ----------

    @Test
    void 根据商品ID查询秒杀配置() {
        FlashItem fi = flashItemMapper.findByProductId(1L);
        System.out.println("[测试] 商品1的秒杀配置 => " + fi);
        assertNotNull(fi, "商品1应有对应的秒杀配置");
        assertEquals(new BigDecimal("9999.00"), fi.getFlashPrice());
        assertEquals(10, fi.getRemaining());
        assertEquals(0, fi.getVersion());
    }

    @Test
    void 查询当前生效的秒杀活动() {
        List<FlashItem> items = flashItemMapper.findActive();
        System.out.println("[测试] 当前生效的秒杀活动 => 数量=" + items.size());
        items.forEach(System.out::println);
        assertFalse(items.isEmpty(), "应至少有一个生效中的秒杀活动");
    }

    @Test
    @Transactional
    void 乐观锁扣减库存() {
        FlashItem before = flashItemMapper.findByProductId(1L);
        assertNotNull(before);
        int stockBefore = before.getRemaining();
        int verBefore = before.getVersion();
        System.out.println("[测试] 乐观锁 => 扣减前: 剩余库存=" + stockBefore + ", 版本号=" + verBefore);

        // 正确版本号 → 应扣减成功
        int r1 = flashItemMapper.deductStock(before.getId(), verBefore);
        assertEquals(1, r1, "使用正确版本号应扣减成功");
        FlashItem after = flashItemMapper.findById(before.getId());
        assertEquals(stockBefore - 1, after.getRemaining());
        assertEquals(verBefore + 1, after.getVersion());
        System.out.println("[测试] 乐观锁 => 扣减后: 剩余库存=" + after.getRemaining() + ", 版本号=" + after.getVersion());

        // 过期版本号 → 应扣减失败
        int r2 = flashItemMapper.deductStock(before.getId(), verBefore);
        assertEquals(0, r2, "使用过期版本号应被拒绝");
        System.out.println("[测试] 乐观锁 => 过期版本号已被正确拒绝");
    }

    // ---------- 订单表测试 ----------

    @Test
    @Transactional
    void 新增并查询订单() {
        TradeOrder o = new TradeOrder();
        o.setMemberId(1001L);
        o.setSellerId(1L);
        o.setProductId(1L);
        o.setProductName("RTX 9090 Ti 限定版");
        o.setAmount(new BigDecimal("9999.00"));
        o.setStatus(0);

        int cnt = tradeOrderMapper.save(o);
        System.out.println("[测试] 新增订单 => 影响行数=" + cnt + ", 订单ID=" + o.getId());
        assertEquals(1, cnt);
        assertNotNull(o.getId());

        TradeOrder fetched = tradeOrderMapper.findById(o.getId());
        assertNotNull(fetched);
        assertEquals("RTX 9090 Ti 限定版", fetched.getProductName());
        assertEquals(new BigDecimal("9999.00"), fetched.getAmount());
        System.out.println("[测试] 订单快照 => 商品名=" + fetched.getProductName() + ", 金额=" + fetched.getAmount());
    }

    @Test
    @Transactional
    void 修改订单状态() {
        TradeOrder o = new TradeOrder();
        o.setMemberId(7777L);
        o.setSellerId(1L);
        o.setProductId(1L);
        o.setProductName("RTX 9090 Ti 限定版");
        o.setAmount(new BigDecimal("9999.00"));
        o.setStatus(0);
        tradeOrderMapper.save(o);

        int rows = tradeOrderMapper.changeStatus(o.getId(), 1);
        assertEquals(1, rows);
        TradeOrder updated = tradeOrderMapper.findById(o.getId());
        assertEquals(1, updated.getStatus());
        System.out.println("[测试] 状态变更 => 订单 " + o.getId() + " 已更新为「已付款」");
    }

    // ---------- 防重购表测试 ----------

    @Test
    @Transactional
    void 防重购记录插入与查重() {
        TradeOrder o = new TradeOrder();
        o.setMemberId(1001L);
        o.setSellerId(1L);
        o.setProductId(1L);
        o.setProductName("RTX 9090 Ti 限定版");
        o.setAmount(new BigDecimal("9999.00"));
        o.setStatus(0);
        tradeOrderMapper.save(o);

        FlashGuard g = new FlashGuard();
        g.setMemberId(1001L);
        g.setOrderId(o.getId());
        g.setProductId(1L);
        int cnt = flashGuardMapper.save(g);
        System.out.println("[测试] 新增防重购记录 => 影响行数=" + cnt + ", 记录ID=" + g.getId());
        assertEquals(1, cnt);

        // 应能查到防重购记录
        FlashGuard found = flashGuardMapper.findByMemberAndProduct(1001L, 1L);
        assertNotNull(found, "防重购记录应存在");
        System.out.println("[测试] 查询防重购记录 => " + found);

        // 不存在的组合应返回 null
        FlashGuard miss = flashGuardMapper.findByMemberAndProduct(8888L, 1L);
        assertNull(miss, "不存在的组合应返回null");

        // 重复插入应触发唯一索引冲突
        FlashGuard dup = new FlashGuard();
        dup.setMemberId(1001L);
        dup.setOrderId(o.getId());
        dup.setProductId(1L);
        assertThrows(Exception.class, () -> flashGuardMapper.save(dup),
                "重复插入应触发唯一索引约束异常");
        System.out.println("[测试] 重复插入 => 已被唯一索引正确拦截");
    }
}
