package com.lhf.usercenter.controller;

import com.lhf.usercenter.model.vo.RankingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/ranking")
@Slf4j
public class RankingController {
    @Autowired
    private RedisTemplate redisTemplate;
    @GetMapping("/add")
    public RankingVO getFollowerRanking() {
        ZSetOperations zSet = redisTemplate.opsForZSet();
        // 如果value不存在，则新增，并设置分数为1
        Boolean added = zSet.addIfAbsent("fansRankingList", "user3", 1);
        if(!added){
            // 如果value存在，则增加分数
            zSet.incrementScore("fansRankingList", "user3", 1);
        }
        Set<ZSetOperations.TypedTuple<Object>> ranking = zSet.reverseRangeWithScores("fansRankingList", 0, -1);
        ranking.forEach(tuple -> {
            log.info("用户：{}，分数：{}", tuple.getValue(), tuple.getScore());
        });
        return new RankingVO();
    }
}