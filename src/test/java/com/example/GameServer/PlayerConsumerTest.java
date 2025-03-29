package com.example.GameServer;

import com.baomidou.mybatisplus.core.batch.BatchMethod;
import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.example.GameServer.Service.Consumer.PlayerConsumer;
import com.example.GameServer.PO.PlayerPO;
import com.example.GameServer.Service.MybatisBatchFactory.MybatisBatchFactory;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DemoApplication.class})
public class PlayerConsumerTest {

    @Mock
    private MybatisBatchFactory batchFactory;
    @Mock
    private MybatisBatch<PlayerPO> mockBatch;
    @Mock
    private SqlSessionFactory sqlSessionFactory;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ConsumerRecord<String, MessagesProto.register_req> record1;
    @Mock
    private ConsumerRecord<String, MessagesProto.register_req> record2;

    // @InjectMocks
    private PlayerConsumer playerConsumer;

    @BeforeEach
    void setUp() {
        // when(sqlSessionFactory.openSession(anyBoolean())).thenReturn(mock(SqlSession.class));
        playerConsumer = new PlayerConsumer(
                batchFactory,
                sqlSessionFactory,
                redisTemplate
        );

    }

    @Test
    public void testProcessRegistration_Success() {
        MessagesProto.register_req proto1 = MessagesProto.register_req.newBuilder()
                .setName("Alice")
                .setEmail("alice@example.com")
                .build();
        MessagesProto.register_req proto2 = MessagesProto.register_req.newBuilder()
                .setName("Bob")
                .setEmail("bob@example.com")
                .build();
        List<MessagesProto.register_req> records = List.of(proto1, proto2);
        when(batchFactory.create(eq(sqlSessionFactory), ArgumentMatchers.<List<PlayerPO>>any())).thenReturn(mockBatch);

        Acknowledgment mockAck = mock(Acknowledgment.class);

        playerConsumer.processRegistration(records, mockAck); // 传入 mockAck

        verify(mockBatch).execute(any(BatchMethod.class));

        verify(redisTemplate).delete("player:email:alice@example.com");
        verify(redisTemplate).expire("player:email:alice@example.com", 1L, TimeUnit.SECONDS);
        verify(redisTemplate).delete("player:email:bob@example.com");
        verify(redisTemplate).expire("player:email:bob@example.com", 1L, TimeUnit.SECONDS);

        verify(mockAck).acknowledge();
    }

    @Ignore
    @Test
    public void testProcessRegistration_EmptyRecords() {
        //List<ConsumerRecord<String, MessagesProto.register_req>> emptyRecords = Collections.emptyList();
        List<MessagesProto.register_req> emptyRecords = Collections.emptyList();
//        playerConsumer.processRegistration(emptyRecords);
//
//        verifyNoInteractions(mockBatch, redisTemplate);
    }

    @Ignore
    @Test
    public void testProcessRegistration_DatabaseFailure() {
        MessagesProto.register_req proto1 = MessagesProto.register_req.newBuilder()
                .setName("Alice")
                .setEmail("alice@example.com")
                .build();
        // when(record1.value()).thenReturn(proto1);
        List<MessagesProto.register_req> records = List.of(proto1);
//        when(batchFactory.create(eq(sqlSessionFactory), ArgumentMatchers.<List<PlayerPO>>any())).thenReturn(mockBatch);
//        doThrow(new RuntimeException("DB error")).when(mockBatch).execute(any(BatchMethod.class));
//
//        assertThrows(RuntimeException.class, () -> {
//            playerConsumer.processRegistration(records);
//        });

        verify(redisTemplate, never()).delete(anyString());
    }
}