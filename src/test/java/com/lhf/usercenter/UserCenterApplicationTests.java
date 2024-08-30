package com.lhf.usercenter;

import com.lhf.usercenter.common.utils.MailUtils;
import com.lhf.usercenter.common.utils.ObsUtil;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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

    @Test
    void testSendMail() {
        MailUtils.sendMail("lhf97777@gmail.com", "123");
    }

}