package com.lhf.usercenter.config;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class RedissonConfigTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {
        // Java中的List
        List<String> list = new ArrayList<>();
        list.add("1");
        System.out.println("Java中的list数据：" + list);

        // Redisson中的List
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("1");
        System.out.println("Redisson中的list数据：" + list);
    }

    @Test
    void testWatchDog() {
        RLock lock = redissonClient.getLock("meetfriends:precachejob:docache:lock");
        try {
            // 默认过期时间为30秒，测试watch dog自动续期机制
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                // 线程睡眠
                Thread.sleep(30000);
                System.out.println("获取到锁，开始执行缓存预热" + Thread.currentThread().getId());
                // 3、为重点用户预热数据
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 4、释放自己的锁，防止死锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("释放锁" + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}