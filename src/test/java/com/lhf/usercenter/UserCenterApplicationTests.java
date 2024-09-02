package com.lhf.usercenter;

import com.lhf.usercenter.common.utils.BaiduUtils;
import com.lhf.usercenter.common.utils.MailUtils;
import com.lhf.usercenter.contant.UserConstant;
import com.lhf.usercenter.model.domain.ReturnLocationBean;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;


import javax.annotation.Resource;

@SpringBootTest
@AutoConfigureMockMvc
class UserCenterApplicationTests {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

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
        MailUtils.sendMail("15671389027@163.com", "123", UserConstant.USER_REGISTER);
    }

    @Test
    void testAdress() {
        String adress = "湖北省武汉市蔡甸区珠山湖达到丽水天成";
        ReturnLocationBean returnLocationBean = BaiduUtils.addressToLongitude(adress);
        System.out.println(returnLocationBean);

    }
}