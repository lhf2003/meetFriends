package com.lhf.usercenter.once;

import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;


@SpringBootTest
class ImportDataTest {

    @Resource
    private UserService userService;

    private final ExecutorService executor = new ThreadPoolExecutor(16, 16, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.AbortPolicy());

    /**
     * 同步导入
     */

    @Test
    void testImportData() {
        StopWatch stopWatch = new StopWatch();
        List<User> userlist = null;
        // 插入十次
        stopWatch.start(); // 开始计时
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
                user.setPhone("14141343212");
                user.setUserProfile("我是假数据");
                user.setEmail("123@wqe.com");
                user.setUserAvatar("https://brandlogos.net/wp-content/uploads/2021/11/java-logo.png");
                user.setTags(Collections.singletonList("\"假数据\"").toString());
                userlist.add(user);
            }
            userService.saveBatch(userlist);
        }
        stopWatch.stop(); // 停止计时
        System.out.println("插入十万条数据总耗时：" + stopWatch.getTotalTimeSeconds()); // 插入十万条数据总耗时：19.115535
    }

    /**
     * 异步导入
     */
    @Test
    void testImportDataByConcurrent() {
        StopWatch stopWatch = new StopWatch();
        // 插入十次
        stopWatch.start(); // 开始计时

        List<User> userlist = new ArrayList<>();
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        try {
            for (int i = 0; i < 10; i++) {
                userlist.clear(); // 复用 ArrayList
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
                futures[i] = CompletableFuture.runAsync(() -> {
                    boolean result = userService.saveBatch(userlist);
                    if (result) {
                        System.out.println("插入成功");
                    } else {
                        System.out.println("插入失败");
                    }
                }, executor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CompletableFuture.allOf(futures);
        stopWatch.stop(); // 停止计时
        // 输出总耗时
        System.out.println("插入十万条数据总耗时：" + stopWatch.getTotalTimeSeconds());
    }
}