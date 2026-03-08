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
 * 库存预热任务
 * 应用启动时将所有生效中的秒杀商品库存加载到 Redis，
 * 使得秒杀热路径无需访问数据库即可完成库存检查。
 */
@Component
@RequiredArgsConstructor
public class InventoryWarmUpTask implements ApplicationRunner {

    /** Redis 库存缓存 key 前缀 */
    public static final String INV_KEY = "fs:inv:";

    /** Redis 已购买用户集合 key 前缀 */
    public static final String BOUGHT_KEY = "fs:purchased:";

    private final FlashItemMapper flashItemMapper;
    private final StringRedisTemplate redis;

    @Override
    public void run(ApplicationArguments args) {
        List<FlashItem> items = flashItemMapper.findActive();
        for (FlashItem fi : items) {
            redis.opsForValue().set(INV_KEY + fi.getId(), String.valueOf(fi.getRemaining()));
        }
        System.out.println("[预热] 已缓存 " + items.size() + " 个秒杀商品的库存信息");
    }
}
