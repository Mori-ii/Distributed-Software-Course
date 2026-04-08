package com.seckill.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.seckill.dto.Result;
import com.seckill.entity.Shop;
import com.seckill.mapper.ShopMapper;
import com.seckill.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.utils.CacheClient;
import com.seckill.utils.RedisData;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.seckill.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
//        缓存穿透
//        Shop shop = cacheClient.queryWithPathThrough(CACHE_SHOP_KEY, id, Shop.class,
//                this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

//        互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

//        逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicExpire(CACHE_SHOP_KEY, id, Shop.class,
                this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
//         7. 返回
        return Result.ok(shop);

    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public Shop queryWithLogicExpire(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询 商户 缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

//        2.判断是否存在
        if(StrUtil.isBlank(shopJson)){
            //3.未命中，返回
            return null;
        }
//        命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop  shop = JSONUtil.toBean((JSONObject)redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
//        5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){

        return shop;
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
//                重建缓存
                    this.saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
        }

//        6.4返回店铺信息
        return shop;
    }


    public Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询 商户 缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

//        2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //        3.存在，返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

//        判断命中的是否是空值
        if(shopJson != null){
//            返回错误信息
            return null;
        }

//        实现缓存重建
//        0.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLocked = tryLock(lockKey);
//        0.2判断是否获取锁成功
            if(!isLocked) {

    //        0.3失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
//        0.4获取锁成功，实现逻辑，根据id查询数据库
//        4.不存在，根据id查询数据库
            shop = getById(id);
            Thread.sleep(200);
//        5.不存在，返回错误
            if(shop == null){
    //            将空值缓存到redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
    //            再返回错误
                return null;
            }

//        6.查询数据库，存在，则返回并存到redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
//         7. 释放互斥锁
            unLock(lockKey);
        }
//        8. 返回
        return shop;

    }


    public Shop queryWithPathThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询 商户 缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

//        2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //        3.存在，返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

//        判断命中的是否是空值
        if(shopJson != null){
//            返回错误信息
            return null;
        }

//        4.不存在，根据id查询数据库
        Shop shop = getById(id);
//        5.不存在，返回错误
        if(shop == null){
//            将空值缓存到redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            再返回错误
            return null;
        }

//        6.查询数据库，存在，则返回并存到redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//         7. 返回
        return shop;

    }


    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id,Long expireSeconds) {
//        1. 查询店铺数据
        Shop shop = getById(id);
//        2.封装成逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        3. 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));
    }


    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
//        1.更新数据库
        updateById(shop);
//        2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
