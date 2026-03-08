package cn.edu.distcourse.flashsale.bootstrap;

import cn.edu.distcourse.flashsale.dao.FlashItemMapper;
import cn.edu.distcourse.flashsale.model.FlashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pre-loads active flash-item inventory into Redis on application startup
 * so that the hot path never needs to hit the database for stock checks.
 */
@Component
@RequiredArgsConstructor
public class InventoryWarmUpTask implements ApplicationRunner {

    public static final String INV_KEY = "fs:inv:";
    public static final String BOUGHT_KEY = "fs:purchased:";

    private final FlashItemMapper flashItemMapper;
    private final StringRedisTemplate redis;

    @Override
    public void run(ApplicationArguments args) {
        List<FlashItem> items = flashItemMapper.findActive();
        for (FlashItem fi : items) {
            redis.opsForValue().set(INV_KEY + fi.getId(), String.valueOf(fi.getRemaining()));
        }
        System.out.println("[WarmUp] Cached inventory for " + items.size() + " flash item(s).");
    }
}
