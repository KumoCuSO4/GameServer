package com.example.GameServer.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.GameServer.MessagesProto;
import com.example.GameServer.PlayerServiceGrpc;
import com.example.GameServer.Mapper.PlayerItemMapper;
import com.example.GameServer.Mapper.PlayerMapper;
import com.example.GameServer.PO.PlayerItemPO;
import com.example.GameServer.PO.PlayerPO;
import com.example.GameServer.utils.SnowflakeIdWorker;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@GrpcService
public class PlayerService extends PlayerServiceGrpc.PlayerServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private PlayerItemMapper playerItemMapper;
    @Autowired
    private KafkaTemplate<String, MessagesProto.register_req> kafkaTemplate;
    @Autowired
    private SnowflakeIdWorker idWorker;

    public PlayerPO getPlayerByUid(long uid) {
        var ops = redisTemplate.opsForValue();
        String key = "player:" + uid;
        boolean hasKey = redisTemplate.hasKey(key);
        PlayerPO playerPO;
        if(hasKey) {
            playerPO = (PlayerPO) ops.get(key);
            log.info("PlayerService:getPlayerByUid() cache get: " + playerPO.toString());
            return playerPO;
        }
        playerPO = playerMapper.selectById(uid);
        if(playerPO != null) {
            ops.set(key, playerPO, 60, TimeUnit.SECONDS);
            log.info("PlayerService:getPlayerByUid() cache insert: " + playerPO.toString());
            return playerPO;
        }
        else{
            throw new RuntimeException("PlayerService:getPlayerByUid() player is null: " + uid);
        }
    }

    @Override
    public void getPlayerByUid(MessagesProto.getPlayerByUid_req request, StreamObserver<MessagesProto.getPlayerByUid_res> responseObserver) {
        PlayerPO playerPO = getPlayerByUid(request.getUid());
        MessagesProto.getPlayerByUid_res response = MessagesProto.getPlayerByUid_res.newBuilder()
                .setUid(playerPO.getUid()).setName(playerPO.getName()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 注册
    @Override
    public void register(MessagesProto.register_req request, StreamObserver<MessagesProto.register_res> responseObserver) {

        // Redis唯一性校验
        String emailKey = "player:email:" + request.getEmail();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(emailKey, "lock", 30, TimeUnit.SECONDS);

        if (locked == null) {
            log.error("Redis connection failed for email: {}", request.getEmail());
            responseObserver.onError(Status.UNAVAILABLE.withDescription("Service unavailable").asRuntimeException());
            return;
        }

        if (locked) {
            if (playerMapper.selectCount(
                    new QueryWrapper<PlayerPO>().eq("email", request.getEmail())
            ) > 0) {
//                responseObserver.onError(Status.ALREADY_EXISTS
//                        .withDescription("Email already exists (db check)")
//                        .asRuntimeException());
                responseObserver.onNext(MessagesProto.register_res.newBuilder()
                        .setStatus(1)
                        .setMessage("Email already exists (db check)")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            try {
                long uid = idWorker.nextId();
                // 进入消息队列
                kafkaTemplate.send("player-register-topic", request);
                responseObserver.onNext(MessagesProto.register_res.newBuilder()
                        .setStatus(0)
                        .setUid(uid)
                        .build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error(e.getMessage());
                redisTemplate.delete(emailKey);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Registration failed")
                        .asRuntimeException());
            }
        } else {
            log.info("PlayerService:register() cache exist: " + request.getEmail());
            responseObserver.onNext(MessagesProto.register_res.newBuilder()
                    .setStatus(1)
                    .setMessage("Email already exists")
                    .build());
            responseObserver.onCompleted();
//            responseObserver.onError(Status.ALREADY_EXISTS
//                    .withDescription("Email already exists")
//                    .asRuntimeException());
        }
    }

    // 玩家获取物品
    @Override
    public void playerGetItem(MessagesProto.playerGetItem_req request, StreamObserver<MessagesProto.playerGetItem_res> responseObserver) {
        LambdaUpdateWrapper<PlayerItemPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("num = num+{0}", request.getItemNum())
                .eq(PlayerItemPO::getUid, request.getUid())
                .eq(PlayerItemPO::getItemId, request.getItemId());
        MessagesProto.playerGetItem_res response;
        int rowsUpdated = playerItemMapper.update(null, updateWrapper);
        if(rowsUpdated == 0) {
            PlayerItemPO playerItemPO = new PlayerItemPO(request.getUid(), request.getItemId(), request.getItemNum());
            playerItemMapper.insert(playerItemPO);
            response = MessagesProto.playerGetItem_res.newBuilder()
                    .setItemId(request.getItemId()).setItemNum(request.getItemNum()).build();
        }
        else {
            PlayerItemPO playerItemPO = playerItemMapper.selectOne(new LambdaQueryWrapper<PlayerItemPO>()
                    .select(PlayerItemPO::getNum)
                    .eq(PlayerItemPO::getUid, request.getUid())
                    .eq(PlayerItemPO::getItemId, request.getItemId()));
            response = MessagesProto.playerGetItem_res.newBuilder()
                    .setItemId(request.getItemId()).setItemNum(playerItemPO.getNum()).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 获取玩家物品列表
    @Override
    public void getItemList(MessagesProto.getItemList_req request, StreamObserver<MessagesProto.getItemList_res> responseObserver) {
        LambdaQueryWrapper<PlayerItemPO> lambdaQuery = new LambdaQueryWrapper<>();
        lambdaQuery.eq(PlayerItemPO::getUid, request.getUid());
        List<PlayerItemPO> list = playerItemMapper.selectList(lambdaQuery);
        List<MessagesProto.player_item> resList = list.stream()
                .map(po -> MessagesProto.player_item.newBuilder()
                        .setUid(po.getUid())
                        .setItemId(po.getItemId())
                        .setItemNum(po.getNum())
                        .build())
                .collect(Collectors.toList());
        MessagesProto.getItemList_res response = MessagesProto.getItemList_res.newBuilder()
                .addAllInfo(resList)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
