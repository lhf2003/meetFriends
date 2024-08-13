package com.lhf.usercenter.once;

import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@SpringBootTest
class ImportDataTest {

    @Resource
    private UserService userService;

    @Test
    void testImportData() {
        StopWatch stopWatch = new StopWatch();
        List<User> userlist = null;
        // 插入十次
        stopWatch.start(); // 开始计时
        for (int i = 0; i < 50; i++) {
            userlist = new ArrayList<>();
            int dataSize = 0;
            // 每次插入10000条数据
            while (true) {
                if (++dataSize > 10000) {
                    break;
                }
                User user = new User();
                user.setUserName("假名");
                user.setUserAccount("假账号啊");
                user.setUserPassword("12345678");
                user.setAge(10);
                user.setSex(1);
                user.setPhone("14141343254");
                user.setUserProfile("我是假数据");
                user.setEmail("123@wqe.com");
                user.setUserAvatar("https://brandlogos.net/wp-content/uploads/2021/11/java-logo.png");
                user.setTags(Collections.singletonList("\"假数据\"").toString());
                userlist.add(user);
            }
            userService.saveBatch(userlist);
        }
        stopWatch.stop(); // 停止计时
        // 耗时24秒左右
        System.out.println("插入十万条数据总耗时：" + stopWatch.getTotalTimeSeconds()); // 输出总耗时
    }

    @Test
    void testImportDataByConcurrent() {
        StopWatch stopWatch = new StopWatch();
        // 插入十次
        stopWatch.start(); // 开始计时
        CompletableFuture.runAsync(() -> {
            List<User> userlist = null;
            for (int i = 0; i < 10; i++) {
                userlist = new ArrayList<>();
                int dataSize = 0;
                // 每次插入10000条数据
                while (true) {
                    if (++dataSize > 10000) {
                        break;
                    }
                    User user = new User();
                    user.setUserName("假名");
                    user.setUserAccount("假账号啊");
                    user.setUserPassword("12345678");
                    user.setAge(10);
                    user.setSex(1);
                    user.setPhone("14141343254");
                    user.setUserProfile("我是假数据");
                    user.setEmail("123@wqe.com");
                    user.setUserAvatar("https://brandlogos.net/wp-content/uploads/2021/11/java-logo.png");
                    user.setTags(Collections.singletonList("\"假数据\"").toString());
                    userlist.add(user);
                }
                boolean result = userService.saveBatch(userlist);
                if (result) {
                    System.out.println("插入成功");
                } else {
                    System.out.println("插入失败");
                }
            }
        });

        stopWatch.stop(); // 停止计时
        // 耗时5~6秒左右
        System.out.println("插入十万条数据总耗时：" + stopWatch.getTotalTimeSeconds()); // 输出总耗时
    }
}