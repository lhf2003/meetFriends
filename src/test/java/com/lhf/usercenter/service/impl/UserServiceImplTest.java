package com.lhf.usercenter.service.impl;

import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class UserServiceImplTest {
    @Resource
    private UserService userService;

    @Test
    void searchUserByTagsName() {
        ArrayList<String> list = new ArrayList<>();
        list.add("java");
        list.add("python");

        List<User> userList = userService.searchUserByTagsName(list);
        System.out.println(userList);
        Assertions.assertNotNull(userList);
    }
}