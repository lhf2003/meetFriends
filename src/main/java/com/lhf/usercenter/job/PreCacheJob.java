package com.lhf.usercenter.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhf.usercenter.contant.UserContant;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lhf.usercenter.contant.UserContant.ALL_USER_CACHE_KEY;
import static com.lhf.usercenter.contant.UserContant.RECOMMEND_CACHE_KEY_PREFIX;

@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;
    // 重点用户列表（为这些用户预热数据）
    final static List<Long> mainUserList = Arrays.asList(1L, 2L);

    // 定时预热数据，每天一点执行一次
    @Scheduled(cron = "0 12 22 * * ?")
    public void doCache() {
        boolean flag = true;
        // 1、定义分布式锁key
        RLock lock = redissonClient.getLock(UserContant.PRE_CACHE_LOCK_KEY);
        try {
            // 2、加锁，谁先到谁先执行，后来者等待，这里等待时间设置为0，因为是每天一次性的定时任务，后来者不需要等待去抢锁
            // 过期时间默认为30秒，防止死锁
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                log.info("获取到锁，开始执行缓存预热" + Thread.currentThread().getId());
                // 3、为重点用户预热数据
                for (Long userId : mainUserList) {
                    // 脱敏
                    List<User> userList = userService.list(new QueryWrapper<>());
                    List<User> safetyUserList = userList.stream().map(userService::getSafetyUser).collect(Collectors.toList());

                    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
                    // 缓存一份全局的用户数据，只缓存一次
                    if (flag) {
                        ops.set(ALL_USER_CACHE_KEY, safetyUserList, 24, TimeUnit.HOURS);
                        flag = false;
                    }
                    // 缓存key：根据用户id生成缓存key
                    String key = RECOMMEND_CACHE_KEY_PREFIX + userId;

                    // 存入缓存,一定要指定过期时间，防止缓存雪崩！！！！！！！！！！！！！！！
                    try {
                        ops.set(key, safetyUserList, 24, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("缓存预热失败", e);
            throw new RuntimeException(e);
        } finally {
            // 4、释放自己的锁，防止死锁
            if (lock.isHeldByCurrentThread()) {
                log.info("释放锁" + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }
}