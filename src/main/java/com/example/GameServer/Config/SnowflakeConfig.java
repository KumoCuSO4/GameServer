package com.example.GameServer.Config;

import com.example.GameServer.utils.SnowflakeIdWorker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {
    @Value("${snowflake.worker-id:0}")
    private long workerId;

    @Value("${snowflake.datacenter-id:0}")
    private long datacenterId;

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker() {
        return new SnowflakeIdWorker(workerId, datacenterId);
    }
}