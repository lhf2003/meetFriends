package com.lhf.usercenter.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingVO {
    /**
     * 粉丝数排行榜
     **/
    private List<UserInfo> fansRanking;
    /**
     * 关注数排行榜
     **/
    private List<UserInfo> followersRanking;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class UserInfo {
        private Integer userId;
        private String username;
        private String avatarUrl;
        private int count;
    }
}