package com.example.GameServer;

import com.example.GameServer.Config.KafkaConfig;
import com.example.GameServer.Controller.RedisController;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = {DemoApplication.class})
class DemoApplicationTests {

	@Resource
	private RedisController redisController;
	@Autowired
	private KafkaTemplate<String, MessagesProto.register_req> kafkaTemplate;

	// 使用redis
//	@Test
//	void test1() {
//		redisController.setValue("Name", "abc");
//		System.out.println(redisController.getValue("Name"));
//	}


	@Test
	public void sendTestMessage() {
		MessagesProto.register_req request = MessagesProto.register_req.newBuilder()
				.setName("TestUser")
				.setEmail("test@example.com")
				.build();
		kafkaTemplate.send("player-register-topic", request);
		System.out.println("Sent test message: " + request);
	}
}
