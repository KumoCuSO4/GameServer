package com.example.GameServer.Service.Consumer;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.example.GameServer.MessagesProto;
import com.example.GameServer.Mapper.PlayerMapper;
import com.example.GameServer.PO.PlayerPO;
import com.example.GameServer.Service.MybatisBatchFactory.MybatisBatchFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class PlayerConsumer {
    @Autowired
    private PlayerMapper playerMapper;

    private final MybatisBatchFactory batchFactory;
    private final SqlSessionFactory sqlSessionFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public PlayerConsumer(
            MybatisBatchFactory batchFactory,
            SqlSessionFactory sqlSessionFactory,
            RedisTemplate<String, Object> redisTemplate) {
        this.batchFactory = batchFactory;
        this.sqlSessionFactory = sqlSessionFactory;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "player-register-topic", groupId = "player-register-group")
    public void processRegistration(List<ConsumerRecord<String, MessagesProto.register_req>> records) {
        List<PlayerPO> players = records.stream()
                .map(record -> PlayerPO.builder()
                        .name(record.value().getName())
                        .email(record.value().getEmail())
                        .build())
                .collect(Collectors.toList());

        if(!players.isEmpty()) {
            MybatisBatch<PlayerPO> mybatisBatch = batchFactory.create(sqlSessionFactory, players);
            MybatisBatch.Method<PlayerPO> method = new MybatisBatch.Method<>(PlayerMapper.class);
            mybatisBatch.execute(method.insert());

            // 延迟双删
            players.forEach(player -> {
                redisTemplate.delete("player:email:" + player.getEmail());
                redisTemplate.expire("player:email:" + player.getEmail(), 1, TimeUnit.SECONDS);
            });
        }
    }
}