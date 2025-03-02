package com.example.GameServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.GameServer.MessagesProto;
import com.example.GameServer.PlayerServiceGrpc;
import com.example.GameServer.mapper.PlayerItemMapper;
import com.example.GameServer.mapper.PlayerMapper;
import com.example.GameServer.po.PlayerItemPO;
import com.example.GameServer.po.PlayerPO;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@GrpcService
public class PlayerService extends PlayerServiceGrpc.PlayerServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);
    @Autowired
    private RedisTemplate<String, PlayerPO> redisTemplate;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private PlayerItemMapper playerItemMapper;


    public PlayerPO getPlayerByUid(int uid) {
        var ops = redisTemplate.opsForValue();
        String key = "player:" + uid;
        boolean hasKey = redisTemplate.hasKey(key);
        PlayerPO playerPO;
        if(hasKey) {
            playerPO = ops.get(key);
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

    @Override
    public void register(MessagesProto.register_req request, StreamObserver<MessagesProto.register_res> responseObserver) {
        PlayerPO playerPO = new PlayerPO(null, request.getName());
        int s = playerMapper.insert(playerPO);
        MessagesProto.register_res response = MessagesProto.register_res.newBuilder()
                .setSuccess(true).setUid(playerPO.getUid()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 为玩家添加物品
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
                    .setSuccess(true).setItemId(request.getItemId()).setItemNum(request.getItemNum()).build();
        }
        else {
            PlayerItemPO playerItemPO = playerItemMapper.selectOne(new LambdaQueryWrapper<PlayerItemPO>()
                    .select(PlayerItemPO::getNum)
                    .eq(PlayerItemPO::getUid, request.getUid())
                    .eq(PlayerItemPO::getItemId, request.getItemId()));
            response = MessagesProto.playerGetItem_res.newBuilder()
                    .setSuccess(true).setItemId(request.getItemId()).setItemNum(playerItemPO.getNum()).build();
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
