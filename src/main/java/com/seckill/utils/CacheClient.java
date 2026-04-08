package com.seckill.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.seckill.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.seckill.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit) {
//        设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
//        写到redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    public <R,ID> R queryWithPathThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1.从redis查询 商户 缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

//        2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //        3.存在，返回
            return JSONUtil.toBean(shopJson,type);
        }

//        判断命中的是否是空值
        if(shopJson != null){
//            返回错误信息
            return null;
        }

//        4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
//        5.不存在，返回错误
        if(r == null){
//            将空值缓存到redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            再返回错误
            return null;
        }

//        6.查询数据库，存在，则返回并存到redis
        this.set(key,r,time,unit);
//         7. 返回
        return r;

    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R queryWithLogicExpire(
            String keyPrefix,Long id,Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1.从redis查询 商户 缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

//        2.判断是否存在
        if(StrUtil.isBlank(shopJson)){
            //3.未命中，返回
            return null;
        }
//        命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject)redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
//        5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){

            return r;
//        5.1未过期，直接 返回 店铺 信息
        }
//        5.2 已过期，需要缓存重建
//        6. 重新缓存
//        6.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLocked = tryLock(lockKey);
//        6.2判断是否获取锁成功
        if(!isLocked){
            //        6.3成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
//                重建缓存,先查数据库，再写入redis
                    R r1 = dbFallback.apply((ID) id);
                    this.setWithLogicExpire(key,r1,time,unit);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
        }
//        6.4返回店铺信息
        return r;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
