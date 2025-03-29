package com.example.GameServer.Service.Consumer;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.example.GameServer.MessagesProto;
import com.example.GameServer.Mapper.PlayerMapper;
import com.example.GameServer.PO.PlayerPO;
import com.example.GameServer.Service.MybatisBatchFactory.MybatisBatchFactory;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class PlayerConsumer {
    private static final Logger log = LoggerFactory.getLogger(PlayerConsumer.class);
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

    @KafkaListener(topics = "player-register-topic", groupId = "player-register-group", containerFactory = "kafkaListenerContainerFactory")
    public void processRegistration(List<MessagesProto.register_req> messages, Acknowledgment ack) {
        try {
            List<PlayerPO> players = messages.stream()
                    .map(req -> PlayerPO.builder()
                            .name(req.getName())
                            .email(req.getEmail())
                            .build())
                    .collect(Collectors.toList());

            if (!players.isEmpty()) {
                log.info(String.valueOf(players.size()));
                // 批量插入数据库
                MybatisBatch<PlayerPO> mybatisBatch = batchFactory.create(sqlSessionFactory, players);
                MybatisBatch.Method<PlayerPO> method = new MybatisBatch.Method<>(PlayerMapper.class);
                mybatisBatch.execute(method.insert());

                // 延迟双删 Redis 缓存
                players.forEach(player -> {
                    String emailKey = "player:email:" + player.getEmail();
                    redisTemplate.delete(emailKey);
                    redisTemplate.expire(emailKey, 1, TimeUnit.SECONDS);
                });
            }

            // 手动提交偏移量（确保处理完成后再提交）
            ack.acknowledge();

        } catch (PersistenceException e) {
            log.error("PersistenceException", e);
            ack.acknowledge(); // 跳过重复数据，提交偏移量

        } catch (Exception e) {
            log.error("Unexpected error in processRegistration", e);
        }
    }
}