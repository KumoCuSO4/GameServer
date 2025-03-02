package com.example.GameServer;

import com.example.GameServer.controller.RedisController;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {DemoApplication.class})
class DemoApplicationTests {

	@Resource
	private RedisController redisController;

	// 使用redis
	@Test
	void test1() {
		redisController.setValue("Name", "abc");
		System.out.println(redisController.getValue("Name"));
	}

}
