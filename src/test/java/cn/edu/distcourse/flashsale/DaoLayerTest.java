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
 * Integration tests for the DAO (Mapper) layer.
 * Each test marked @Transactional rolls back automatically so the database stays clean.
 */
@SpringBootTest
class DaoLayerTest {

    @Autowired private MemberMapper memberMapper;
    @Autowired private SellerMapper sellerMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private FlashItemMapper flashItemMapper;
    @Autowired private TradeOrderMapper tradeOrderMapper;
    @Autowired private FlashGuardMapper flashGuardMapper;

    // ---------- MemberMapper ----------

    @Test
    void lookupMemberById() {
        Member m = memberMapper.findById(1001L);
        System.out.println("[Test] lookupMemberById => " + m);
        assertNotNull(m, "Member 1001 should exist");
        assertEquals("TestUser_Alpha", m.getAlias());
    }

    @Test
    @Transactional
    void insertMember() {
        Member m = new Member();
        m.setAlias("lab_tester");
        m.setPwd("e10adc3949ba59abbe56e057f20f883e");
        m.setSalt("abcdef");

        int cnt = memberMapper.save(m);
        System.out.println("[Test] insertMember => rows=" + cnt + ", id=" + m.getId());
        assertEquals(1, cnt);
        assertNotNull(m.getId());
    }

    // ---------- SellerMapper ----------

    @Test
    void lookupSellerById() {
        Seller s = sellerMapper.findById(1L);
        System.out.println("[Test] lookupSellerById => " + s);
        assertNotNull(s, "Seller 1 should exist");
    }

    @Test
    void listActiveSellers() {
        List<Seller> list = sellerMapper.findAllActive();
        System.out.println("[Test] activeSellers => count=" + list.size());
        list.forEach(System.out::println);
        assertFalse(list.isEmpty());
    }

    // ---------- ProductMapper ----------

    @Test
    void lookupProductById() {
        Product p = productMapper.findById(1L);
        System.out.println("[Test] lookupProductById => " + p);
        assertNotNull(p, "Product 1 should exist");
        assertEquals(1L, p.getSellerId());
        System.out.println("  name=" + p.getProductName() + ", price=" + p.getPrice() + ", stock=" + p.getStock());
    }

    @Test
    void listProductsBySeller() {
        List<Product> list = productMapper.findBySellerId(1L);
        System.out.println("[Test] productsBySeller(1) => count=" + list.size());
        list.forEach(System.out::println);
        assertFalse(list.isEmpty());
    }

    @Test
    @Transactional
    void insertProduct() {
        Product p = new Product();
        p.setSellerId(1L);
        p.setProductName("Lab Sample Mug");
        p.setImageUrl("https://example.com/mug.png");
        p.setPrice(new BigDecimal("49.90"));
        p.setStock(100);

        int cnt = productMapper.save(p);
        System.out.println("[Test] insertProduct => rows=" + cnt + ", id=" + p.getId());
        assertEquals(1, cnt);
        assertNotNull(p.getId());
    }

    // ---------- FlashItemMapper (core) ----------

    @Test
    void lookupFlashItemByProductId() {
        FlashItem fi = flashItemMapper.findByProductId(1L);
        System.out.println("[Test] flashItemByProduct(1) => " + fi);
        assertNotNull(fi, "Flash config for product 1 should exist");
        assertEquals(new BigDecimal("9999.00"), fi.getFlashPrice());
        assertEquals(10, fi.getRemaining());
        assertEquals(0, fi.getVersion());
    }

    @Test
    void listActiveFlashItems() {
        List<FlashItem> items = flashItemMapper.findActive();
        System.out.println("[Test] activeFlashItems => count=" + items.size());
        items.forEach(System.out::println);
        assertFalse(items.isEmpty(), "There should be at least one active flash item");
    }

    @Test
    @Transactional
    void optimisticLockDeduction() {
        FlashItem before = flashItemMapper.findByProductId(1L);
        assertNotNull(before);
        int stockBefore = before.getRemaining();
        int verBefore = before.getVersion();
        System.out.println("[Test] optimisticLock => before: remaining=" + stockBefore + ", ver=" + verBefore);

        // correct version -> should succeed
        int r1 = flashItemMapper.deductStock(before.getId(), verBefore);
        assertEquals(1, r1, "Deduction with correct version should succeed");
        FlashItem after = flashItemMapper.findById(before.getId());
        assertEquals(stockBefore - 1, after.getRemaining());
        assertEquals(verBefore + 1, after.getVersion());
        System.out.println("[Test] optimisticLock => after:  remaining=" + after.getRemaining() + ", ver=" + after.getVersion());

        // stale version -> should fail
        int r2 = flashItemMapper.deductStock(before.getId(), verBefore);
        assertEquals(0, r2, "Stale version should be rejected");
        System.out.println("[Test] optimisticLock => stale version rejected OK");
    }

    // ---------- TradeOrderMapper ----------

    @Test
    @Transactional
    void insertAndQueryOrder() {
        TradeOrder o = new TradeOrder();
        o.setMemberId(1001L);
        o.setSellerId(1L);
        o.setProductId(1L);
        o.setProductName("RTX 9090 Ti Cyber Edition");
        o.setAmount(new BigDecimal("9999.00"));
        o.setStatus(0);

        int cnt = tradeOrderMapper.save(o);
        System.out.println("[Test] insertOrder => rows=" + cnt + ", id=" + o.getId());
        assertEquals(1, cnt);
        assertNotNull(o.getId());

        TradeOrder fetched = tradeOrderMapper.findById(o.getId());
        assertNotNull(fetched);
        assertEquals("RTX 9090 Ti Cyber Edition", fetched.getProductName());
        assertEquals(new BigDecimal("9999.00"), fetched.getAmount());
        System.out.println("[Test] orderSnapshot => name=" + fetched.getProductName() + ", amount=" + fetched.getAmount());
    }

    @Test
    @Transactional
    void updateOrderStatus() {
        TradeOrder o = new TradeOrder();
        o.setMemberId(7777L);
        o.setSellerId(1L);
        o.setProductId(1L);
        o.setProductName("RTX 9090 Ti Cyber Edition");
        o.setAmount(new BigDecimal("9999.00"));
        o.setStatus(0);
        tradeOrderMapper.save(o);

        int rows = tradeOrderMapper.changeStatus(o.getId(), 1);
        assertEquals(1, rows);
        TradeOrder updated = tradeOrderMapper.findById(o.getId());
        assertEquals(1, updated.getStatus());
        System.out.println("[Test] statusChange => order " + o.getId() + " changed to PAID");
    }

    // ---------- FlashGuardMapper (idempotency) ----------

    @Test
    @Transactional
    void guardInsertAndDuplicateCheck() {
        TradeOrder o = new TradeOrder();
        o.setMemberId(1001L);
        o.setSellerId(1L);
        o.setProductId(1L);
        o.setProductName("RTX 9090 Ti Cyber Edition");
        o.setAmount(new BigDecimal("9999.00"));
        o.setStatus(0);
        tradeOrderMapper.save(o);

        FlashGuard g = new FlashGuard();
        g.setMemberId(1001L);
        g.setOrderId(o.getId());
        g.setProductId(1L);
        int cnt = flashGuardMapper.save(g);
        System.out.println("[Test] guardInsert => rows=" + cnt + ", id=" + g.getId());
        assertEquals(1, cnt);

        // should find the guard record
        FlashGuard found = flashGuardMapper.findByMemberAndProduct(1001L, 1L);
        assertNotNull(found, "Guard record should exist");
        System.out.println("[Test] guardLookup => " + found);

        // non-existent combo should return null
        FlashGuard miss = flashGuardMapper.findByMemberAndProduct(8888L, 1L);
        assertNull(miss, "Non-existent combo should be null");

        // duplicate insert should trigger unique-index violation
        FlashGuard dup = new FlashGuard();
        dup.setMemberId(1001L);
        dup.setOrderId(o.getId());
        dup.setProductId(1L);
        assertThrows(Exception.class, () -> flashGuardMapper.save(dup),
                "Duplicate guard should cause unique-index violation");
        System.out.println("[Test] duplicateGuard => correctly rejected by unique index");
    }
}
