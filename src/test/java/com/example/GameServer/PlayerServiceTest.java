package com.example.GameServer;

import com.example.GameServer.service.PlayerService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback(value = true)
@SpringBootTest(classes = {DemoApplication.class})
public class PlayerServiceTest {
    private Server server;
    private ManagedChannel channel;

    @Autowired
    private PlayerService playerService;

    @BeforeEach
    public void setUp() throws Exception {
        System.out.println("----- PlayerServiceTest BeforeEach ------");

        // 创建一个唯一的服务器名称
        String serverName = InProcessServerBuilder.generateName();

        // 启动内存中的 gRPC 服务
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor() // 使用直接执行器以简化测试
                .addService(playerService)
                .build()
                .start();

        // 创建内存中的 gRPC 客户端通道
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor() // 使用直接执行器以简化测试
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.out.println("----- PlayerServiceTest AfterEach ------");
        // 关闭服务端和客户端
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    public void test1() {
        System.out.println("----- PlayerServiceTest getPlayerByUid ------");
        PlayerServiceGrpc.PlayerServiceBlockingStub stub = PlayerServiceGrpc.newBlockingStub(channel);
        MessagesProto.getPlayerByUid_req request = MessagesProto.getPlayerByUid_req.newBuilder()
                .setUid(1)
                .build();
        MessagesProto.getPlayerByUid_res response = stub.getPlayerByUid(request);
        System.out.println(response);
    }

    @Test
    public void test2() {
        System.out.println("----- PlayerServiceTest register ------");
        PlayerServiceGrpc.PlayerServiceBlockingStub stub = PlayerServiceGrpc.newBlockingStub(channel);
        MessagesProto.register_req request = MessagesProto.register_req.newBuilder()
                .setName("player123")
                .build();
        MessagesProto.register_res response = stub.register(request);
        System.out.println(response);
    }

    @Test
    public void test3() {
        System.out.println("----- PlayerServiceTest getItemList ------");
        PlayerServiceGrpc.PlayerServiceBlockingStub stub = PlayerServiceGrpc.newBlockingStub(channel);
        MessagesProto.getItemList_req request = MessagesProto.getItemList_req.newBuilder()
                .setUid(1)
                .build();
        MessagesProto.getItemList_res response = stub.getItemList(request);
        System.out.println(response);
    }

    @Test
    public void test4() {
        System.out.println("----- PlayerServiceTest playerGetItem ------");
        PlayerServiceGrpc.PlayerServiceBlockingStub stub = PlayerServiceGrpc.newBlockingStub(channel);
        MessagesProto.playerGetItem_req request = MessagesProto.playerGetItem_req.newBuilder()
                .setUid(1)
                .setItemId(1001)
                .setItemNum(100)
                .build();
        MessagesProto.playerGetItem_res response = stub.playerGetItem(request);
        System.out.println(response);
    }


}
