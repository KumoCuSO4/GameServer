package com.example.GameServer;

import com.example.GameServer.Mapper.PlayerMapper;
import com.example.GameServer.PO.PlayerPO;
import com.example.GameServer.Service.PlayerService;
import com.example.GameServer.utils.SnowflakeIdWorker;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DemoApplication.class})
public class PlayerServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private KafkaTemplate<String, MessagesProto.register_req> kafkaTemplate;
    @Mock
    private PlayerMapper playerMapper;
    @Mock
    private SnowflakeIdWorker idWorker;
    @Mock
    private StreamObserver<MessagesProto.register_res> responseObserver;
    @InjectMocks
    private PlayerService playerService;
    @Captor
    private ArgumentCaptor<MessagesProto.register_res> responseCaptor;
    @Captor
    private ArgumentCaptor<StatusRuntimeException> exceptionCaptor;

    private final String testEmail = "test@example.com";
    private final long testUid = 12345L;
    private MessagesProto.register_req request;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        request = MessagesProto.register_req.newBuilder()
                .setEmail(testEmail)
                .build();
    }

    // ===================================testGetPlayerByUid===============================
    @Test
    void testGetPlayerByUid_CacheMiss_DbHit() {
        // when(valueOperations.get("player:1")).thenReturn(null);

        PlayerPO mockPlayer = new PlayerPO(1L, "Alice", "123456@test.com");
        when(playerMapper.selectById(1L)).thenReturn(mockPlayer);

        PlayerPO result = playerService.getPlayerByUid(1L);

        Assertions.assertEquals(mockPlayer, result);
        verify(valueOperations).set(
                eq("player:1"), eq(mockPlayer), eq(60L), eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testGetPlayerByUid_CacheHit() {
        PlayerPO mockPlayer = new PlayerPO(1L, "Alice", "123456@test.com");
        when(redisTemplate.hasKey("player:1")).thenReturn(true);
        when(valueOperations.get("player:1")).thenReturn(mockPlayer);

        PlayerPO result = playerService.getPlayerByUid(1L);

        Assertions.assertEquals(mockPlayer, result);
        verify(valueOperations, times(1)).get("player:1");
        verify(playerMapper, never()).selectById(anyLong()); // 确保未查询数据库
    }


    // ===================================testRegister===============================
    @Test
    void testRegister_Success() {
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), eq("lock"), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(idWorker.nextId()).thenReturn(testUid);

        playerService.register(request, responseObserver);

        verify(redisTemplate.opsForValue()).setIfAbsent(
                eq("player:email:" + testEmail), eq("lock"), eq(30L), eq(TimeUnit.SECONDS));
        verify(kafkaTemplate).send(eq("player-register-topic"), eq(request));
        verify(responseObserver).onNext(responseCaptor.capture());
        assertEquals(testUid, responseCaptor.getValue().getUid());
        verify(responseObserver).onCompleted();
        verifyNoMoreInteractions(responseObserver);
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), eq("lock"), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        playerService.register(request, responseObserver);

        verify(responseObserver).onError(exceptionCaptor.capture());
        StatusRuntimeException exception = exceptionCaptor.getValue();
        assertEquals(Status.ALREADY_EXISTS.getCode(), exception.getStatus().getCode());
        assertEquals("Email already exists", exception.getStatus().getDescription());
        // verify(responseObserver).onCompleted();
    }

    @Test
    void testRegister_RedisLockFailure() {
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), eq("lock"), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(null);

        playerService.register(request, responseObserver);

        verify(responseObserver).onError(exceptionCaptor.capture());
        StatusRuntimeException exception = exceptionCaptor.getValue();
        assertEquals(Status.ALREADY_EXISTS.getCode(), exception.getStatus().getCode());
    }

    @Test
    void testRegister_KafkaFailure() {
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), eq("lock"), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(idWorker.nextId()).thenReturn(testUid);
        doThrow(new RuntimeException("Kafka error")).when(kafkaTemplate).send(anyString(), any());

        playerService.register(request, responseObserver);

        verify(redisTemplate).delete(eq("player:email:" + testEmail));
        verify(responseObserver).onError(exceptionCaptor.capture());
        StatusRuntimeException exception = exceptionCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), exception.getStatus().getCode());
        assertEquals("Registration failed", exception.getStatus().getDescription());
        verify(responseObserver).onCompleted();
    }
}
