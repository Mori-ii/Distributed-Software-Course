package com.seckill.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP= 1640995200L;
    private StringRedisTemplate stringRedisTemplate;
    private static final long COUNT_BITS = 32;
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
//        1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
//        2.生成序列号
//        2.1获取当前日期，精确到 天
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        2.2 获取自增序列号
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":"+date);

//        3. 拼接
        return timestamp << COUNT_BITS | count;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second);
    }
}
