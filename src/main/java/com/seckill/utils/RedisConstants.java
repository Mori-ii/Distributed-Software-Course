package com.seckill.utils;

/**
 * Redis常量类，统一管理所有Key前缀和TTL
 */
public class RedisConstants {
    // 登录验证码
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    // 登录Token
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;
    // 缓存空值
    public static final Long CACHE_NULL_TTL = 2L;
    // 商铺缓存
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    // 商铺锁
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
    // 秒杀库存
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    // 笔记点赞
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    // 推送
    public static final String FEED_KEY = "feed:";
    // 商铺GEO
    public static final String SHOP_GEO_KEY = "shop:geo:";
    // 用户签到
    public static final String USER_SIGN_KEY = "sign:";
}
