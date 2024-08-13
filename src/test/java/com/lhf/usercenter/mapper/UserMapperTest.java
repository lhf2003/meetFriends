package com.lhf.usercenter.mapper;

import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


@SpringBootTest
class UserMapperTest {
    @Resource
    private UserService userService;

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

}