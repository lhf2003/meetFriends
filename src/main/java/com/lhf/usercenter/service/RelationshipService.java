package com.lhf.usercenter.service;

import com.lhf.usercenter.model.domain.Relationship;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.vo.FriendVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author LHF
* @description 针对表【follow_relationship(粉丝关注关系)】的数据库操作Service
* @createDate 2024-08-13 23:51:49
*/
public interface RelationshipService extends IService<Relationship> {

    /**
     * 关注用户
     * @param id 要关注的用户id
     * @param loginUser 当前登录用户
     * @return
     */
    String followUser(Long id, User loginUser);

    /**
     * 取消关注用户
     * @param id 要取消关注的用户id
     * @param loginUser 当前登录用户
     * @return
     */
    String unFollowUser(Long id, User loginUser);

    /**
     * 获取用户粉丝数
     * @param id 用户id
     * @return
     */
    long getFansNum(Long id);

    /**
     * 获取用户的关注数
     *
     * @return
     */
    long getFollowNum(Long id);

    /**
     * 判断当前登录用户是否已关注用户
     * @param id 被关注用户id
     * @param loginUser
     * @return
     */
    boolean isFans(long id, User loginUser);

    /**
     * 获取所有好友（关注/粉丝）
     * @param loginUser 当前登录用户
     * @return
     */
    List<FriendVO> getAllFriends(User loginUser);

    /**
     * 获取粉丝列表
     * @param loginUser 当前登录用户
     * @return 粉丝列表
     */
    List<User> getFans(User loginUser);

    /**
     * 获取关注列表
     * @param loginUser 当前登录用户
     * @return 关注列表
     */
    List<User> getFollowers(User loginUser);

    /**
     * 管理员默认关注新用户
     * @param userId 新用户id
     */
    void setDefaultFans(long userId);
}