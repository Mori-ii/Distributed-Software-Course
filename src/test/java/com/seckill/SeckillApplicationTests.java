package com.seckill;

import com.seckill.entity.Shop;
import com.seckill.service.impl.ShopServiceImpl;
import com.seckill.utils.CacheClient;
import com.seckill.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.seckill.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class SeckillApplicationTests {

    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private CacheClient cacheClient;
    @Resource
    private RedisIdWorker redisIdWorker;
    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testSaveShop() throws InterruptedException{
        shopService.saveShop2Redis(1L,10L);
    }

    @Test
    void testQueryShop() throws InterruptedException{
        Shop shop = shopService.getById(1L);
        cacheClient.set(CACHE_SHOP_KEY + 1L,shop,10L, TimeUnit.SECONDS);
    }

    @Test
    void testIdWorker() throws InterruptedException{
        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for(int i = 0; i < 300; i++){
            es.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }
}
