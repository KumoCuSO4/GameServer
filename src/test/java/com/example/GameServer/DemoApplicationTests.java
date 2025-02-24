package com.example.GameServer;

import com.example.GameServer.controller.RedisController;
import com.example.GameServer.mapper.UserMapper;
import com.example.GameServer.po.UserPO;
import com.example.GameServer.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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

	// 使用mybatis plus访问mysql
	@Autowired
	private UserMapper userMapper;
	@Test
	public void testSelect() {
		System.out.println(("----- selectAll method test ------"));
		List<UserPO> userList = userMapper.selectList(null);
		userList.forEach(System.out::println);
	}


	@Autowired
	private UserService userService;

	@Test
	public void testUserService() {
		System.out.println(("----- UserService method test ------"));
		UserPO userPO = new UserPO();
		userPO.setName("abc");
		int id = userService.addUser(userPO);
		System.out.println(id);
		UserPO userPO1 = userService.getUserById(1);
		System.out.println(userPO1.toString());
	}

}
