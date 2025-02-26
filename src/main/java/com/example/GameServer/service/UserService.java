package com.example.GameServer.service;

import com.example.GameServer.MessagesProto;
import com.example.GameServer.UserServiceGrpc;
import com.example.GameServer.mapper.UserMapper;
import com.example.GameServer.po.UserPO;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@GrpcService
public class UserService extends UserServiceGrpc.UserServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private RedisTemplate<String, UserPO> redisTemplate;
    @Autowired
    private UserMapper userMapper;

    public UserPO getUserById(int id) {
        var ops = redisTemplate.opsForValue();
        String key = "user_" + id;
        boolean hasKey = redisTemplate.hasKey(key);
        UserPO userPO;
        if(hasKey) {
            userPO = (UserPO) ops.get(key);
            log.info("UserService:getUserById() cache get: " + userPO.toString());
            return userPO;
        }
        userPO = userMapper.selectById(id);
        if(userPO != null) {
            ops.set(key, userPO, 300, TimeUnit.SECONDS);
            log.info("UserService:getUserById() cache insert: " + userPO.toString());
            return userPO;
        }
        else{
            throw new RuntimeException("UserService:getUserById() user is null: " + id);
        }
    }

    public int addUser(UserPO userPO) {
        userMapper.insert(userPO);
        return userPO.getId();
    }

    // gRPC
    @Override
    public void getUserById(MessagesProto.getUserByID_req request, StreamObserver<MessagesProto.getUserByID_res> responseObserver) {
        UserPO userPO = getUserById(request.getId());
        MessagesProto.getUserByID_res response = MessagesProto.getUserByID_res.newBuilder().setId(userPO.getId()).setName(userPO.getName()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
