package com.example.GameServer.utils;

public class SnowflakeIdWorker {
    // 起始时间戳（2020-01-01 00:00:00）
    private final long startTime = 1577836800000L;

    // 机器ID占用的位数（5位）
    private final long workerIdBits = 5L;
    // 数据中心ID占用的位数（5位）
    private final long datacenterIdBits = 5L;
    // 序列号占用的位数（12位）
    private final long sequenceBits = 12L;

    // 支持的最大机器ID（0~31）
    private final long maxWorkerId = ~(-1L << workerIdBits);
    // 支持的最大数据中心ID（0~31）
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);

    // 机器ID偏移量
    private final long workerIdShift = sequenceBits;
    // 数据中心ID偏移量
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    // 时间戳偏移量
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    // 序列号掩码（用于取模）
    private final long sequenceMask = ~(-1L << sequenceBits);

    // 工作节点ID
    private long workerId;
    // 数据中心ID
    private long datacenterId;
    // 序列号
    private long sequence = 0L;
    // 最后生成ID的时间戳
    private long lastTimestamp = -1L;

    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId must be between 0 and " + maxWorkerId);
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId must be between 0 and " + maxDatacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // 线程安全的ID生成方法
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 处理时钟回拨（可选抛异常或等待）
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 序列号用尽，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - startTime) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}