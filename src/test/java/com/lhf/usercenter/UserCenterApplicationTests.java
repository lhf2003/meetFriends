package com.lhf.usercenter;

import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
class UserCenterApplicationTests {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void contextLoads() {
        User user = new User();
        user.setUserName("LHF");
        user.setUserAccount("LHF");
        user.setUserPassword("12345678");
        user.setAge(21);
        user.setSex(1);
        user.setPhone("1234567890");
        user.setEmail("1234567890@qq.com");

        boolean save = userService.save(user);
        Assertions.assertTrue(save);
        System.out.println(user.getId());
    }

    @Test
    void testRedisTemplate() {
        //获取操作字符串的对象
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        //添加一个字符串
        ops.set("name", "lhf");
        //添加一个对象
        User user = new User();
        user.setId(1L);
        user.setUserName("LHF");
        user.setUserAccount("LHF");
        user.setUserPassword("12345678");
        user.setAge(21);
        ops.set("user", user.toString());

        //测试获取字符串
        String name = (String) ops.get("name");
        Assertions.assertEquals("lhf", name);

        //测试获取对象
        System.out.println(ops.get("user"));


    }

}
