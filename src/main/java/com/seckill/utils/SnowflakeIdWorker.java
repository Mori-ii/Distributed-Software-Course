package com.seckill.utils;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 结构: 1位符号位 | 41位时间戳 | 5位数据中心ID | 5位机器ID | 12位序列号
 */
@Component
public class SnowflakeIdWorker {

    private static final long EPOCH = 1640995200000L; // 2022-01-01 00:00:00 UTC

    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MACHINE_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);  // 31
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);        // 31
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);            // 4095

    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdWorker() {
        this(1, 1);
    }

    public SnowflakeIdWorker(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId must be in [0, " + MAX_DATACENTER_ID + "]");
        }
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("machineId must be in [0, " + MAX_MACHINE_ID + "]");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long timestamp = currentTime();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards, refusing to generate ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}
