package com.example.GameServer;

import com.example.GameServer.po.UserPO;
import com.example.GameServer.service.UserService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {DemoApplication.class})
public class UserServiceTest {
    private Server server;
    private ManagedChannel channel;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setUp() throws Exception {
        System.out.println("----- UserServiceTest BeforeEach ------");

        // 创建一个唯一的服务器名称
        String serverName = InProcessServerBuilder.generateName();

        // 启动内存中的 gRPC 服务
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor() // 使用直接执行器以简化测试
                .addService(userService)
                .build()
                .start();

        // 创建内存中的 gRPC 客户端通道
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor() // 使用直接执行器以简化测试
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.out.println("----- UserServiceTest AfterEach ------");
        // 关闭服务端和客户端
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    public void testRPC() {
        System.out.println("----- UserServiceTest RPC ------");
        // 创建客户端存根
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

        // 构造请求
        MessagesProto.getUserByID_req request = MessagesProto.getUserByID_req.newBuilder()
                .setId(1)
                .build();

        // 调用服务端方法
        MessagesProto.getUserByID_res response = stub.getUserById(request);

        System.out.println(response);
    }


    @Test
    public void testUserService() {

        UserPO userPO = new UserPO();
        userPO.setName("abc");
        int id = userService.addUser(userPO);
        System.out.println(id);
        UserPO userPO1 = userService.getUserById(1);
        System.out.println(userPO1.toString());
    }
}
