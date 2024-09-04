package com.lhf.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.mapper.RelationshipMapper;
import com.lhf.usercenter.model.domain.Relationship;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.vo.FriendVO;
import com.lhf.usercenter.service.ChatMessagesService;
import com.lhf.usercenter.service.RelationshipService;
import com.lhf.usercenter.service.UserOnlineStatusService;
import com.lhf.usercenter.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author LHF
 * @description 针对表【follow_relationship(粉丝关注关系)】的数据库操作Service实现
 * @createDate 2024-08-13 23:51:49
 */
@Service
public class RelationshipServiceImpl extends ServiceImpl<RelationshipMapper, Relationship>
        implements RelationshipService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    @Resource
    private UserOnlineStatusService userOnlineStatusService;

    @Resource
    private ChatMessagesService chatMessagesService;

    /**
     * 关注用户
     *
     * @param id        要关注的用户id
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public String followUser(Long id, User loginUser) {
        // 校验参数防止空指针异常
        if (id == null || id <= 0 || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 判断是否已关注用户
        long isFollow = isFollowUser(id, loginUser);
        if (isFollow != -1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已关注该用户");
        }
        // 用户不能关注自己
        if (id == loginUser.getId()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能关注自己");
        }

        // 分布式锁
        RLock lock = redissonClient.getLock("meetfriends:follow_user");
        // 等待时间 释放时间 单位
        try {
            while (true) {
                // 只有一个线程能获取到锁
                if (lock.tryLock(0, 30000L, TimeUnit.MILLISECONDS)) {
                    // 插入数据
                    Relationship followRelationship = new Relationship();
                    followRelationship.setFollowerId(loginUser.getId());
                    followRelationship.setFollowedId(id);
                    boolean saveResult = this.save(followRelationship);
                    if (saveResult) {
                        return "关注成功!";
                    } else {
                        return "关注失败!";
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doJoinTeam error", e);
            return "关注失败!";
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) { // 判断当前这个锁是不是这个线程的
                lock.unlock(); // 释放锁
            }
        }
    }

    /**
     * 取消关注用户
     *
     * @param id        要取消关注的用户id
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public String unFollowUser(Long id, User loginUser) {
        // 校验参数非空
        if (id == null || id <= 0 || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 判断是否已关注用户
        long isFollow = isFollowUser(id, loginUser);
        if (isFollow == -1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未关注该用户");
        }
        // 更新数据
        boolean removeResult = this.removeById(isFollow);
        if (removeResult) {
            return "成功取消关注";
        }
        return "取消关注失败";
    }

    /**
     * 获取用户粉丝数
     *
     * @param id 用户id
     * @return
     */
    @Override
    public long getFansNum(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 查询粉丝数量
        QueryWrapper<Relationship> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followedId", id);
        List<Relationship> fansNum = this.list(queryWrapper);
        return fansNum.size();
    }

    /**
     * 获取用户的关注数
     *
     * @return
     */
    @Override
    public long getFollowNum(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 查询关注数量
        QueryWrapper<Relationship> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", id);
        List<Relationship> followerNum = this.list(queryWrapper);
        return followerNum.size();
    }

    /**
     * 判断当前登录用户是否已关注用户
     *
     * @param id        被关注用户id
     * @param loginUser
     * @return
     */
    @Override
    public boolean isFans(long id, User loginUser) {
        long isFollow = isFollowUser(id, loginUser);
        return isFollow != -1;
    }

    /**
     * 判断是否已关注用户
     *
     * @param id        要关注的用户id
     * @param loginUser 当前登录用户id
     * @return
     */
    private long isFollowUser(long id, User loginUser) {
        QueryWrapper<Relationship> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", loginUser.getId());
        queryWrapper.eq("followedId", id);
        List<Relationship> followRelationships = this.list(queryWrapper);
        if (followRelationships != null && !followRelationships.isEmpty()) {
            // 有则返回数据id
            return followRelationships.get(0).getId();
        }
        return -1;
    }

    /**
     * 获取所有好友（关注/粉丝）
     *
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public List<FriendVO> getAllFriends(User loginUser) {

        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        Long loginUserId = loginUser.getId();
        if (loginUserId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 获取当前用户关注的用户以及粉丝
        QueryWrapper<Relationship> followRelationshipQueryWrapper = new QueryWrapper<>();
        followRelationshipQueryWrapper
                .eq("followerId", loginUserId) // 用户关注的用户
                .or()
                .eq("followedId", loginUserId); // 用户的粉丝
        List<Relationship> followRelationships = this.list(followRelationshipQueryWrapper);
        if (followRelationships.isEmpty()) { // 没有数据直接返回
            return new ArrayList<>();
        }

        // 获取用户所有的粉丝
        List<Long> followedUserIds = followRelationships.stream()
                .map(Relationship::getFollowedId)
                .collect(Collectors.toList());
        // 获取用户关注的用户
        List<Long> followerUserIds = followRelationships.stream()
                .map(Relationship::getFollowerId)
                .collect(Collectors.toList());
        // 构造条件获取粉丝/关注用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (!followerUserIds.isEmpty() && !followedUserIds.isEmpty()) {
            userQueryWrapper
                    .in("id", followedUserIds)
                    .or()
                    .in("id", followerUserIds);
        } else if (!followerUserIds.isEmpty()) {
            userQueryWrapper.in("id", followerUserIds);
        } else if (!followedUserIds.isEmpty()) {
            userQueryWrapper.in("id", followedUserIds);
        }

        List<User> friends = userService.list(userQueryWrapper);
        // 排除用户自己
        friends = friends.stream()
                .filter(user -> !user.getId().equals(loginUserId))
                .collect(Collectors.toList());
        // 构造数据
        List<FriendVO> friendVOList = new ArrayList<>();
        friends.stream().forEach(user -> {
            FriendVO friendVO = new FriendVO();
            friendVO.setId(user.getId()); // 设置id
            friendVO.setName(user.getUserName()); // 设置昵称
            friendVO.setAvatar(user.getUserAvatar()); // 设置头像
            // 获取用户在线状态
            Integer userStatus = userOnlineStatusService.getUserStatus(user.getId());
            if (userStatus == null) userStatus = 0; // 默认设置离线
            friendVO.setStatus(userStatus); // 设置用户在线状态
            boolean readStatus = chatMessagesService.getReadStatus(user, loginUser);
            friendVO.setHasUnread(readStatus); // 设置未读消息状态
            friendVOList.add(friendVO); // 添加数据到集合
        });
        return friendVOList;
    }

    /**
     * 获取粉丝列表
     *
     * @param loginUser 当前登录用户
     * @return 粉丝列表
     */
    @Override
    public List<User> getFans(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        long loginUserId = loginUser.getId();
        QueryWrapper<Relationship> followRelationshipQueryWrapper = new QueryWrapper<>();
        followRelationshipQueryWrapper.eq("followedId", loginUserId);
        List<Relationship> followRelationships = this.list(followRelationshipQueryWrapper);
        if (followRelationships == null || followRelationships.isEmpty()) {
            throw new BusinessException(ErrorCode.NULL_DATA, "当前用户没有粉丝");
        }
        // 获取粉丝id列表
        List<Long> fansIds = followRelationships.stream()
                .map(Relationship::getFollowerId)
                .collect(Collectors.toList());

        // 根据id获取粉丝用户集合
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", fansIds);
        List<User> fans = userService.list(userQueryWrapper);
        List<User> safetyFans = new ArrayList<>();

        // 脱敏返回
        for (User follower : fans) {
            User safetyUser = userService.getSafetyUser(follower);
            safetyFans.add(safetyUser);
        }
        return safetyFans;
    }

    /**
     * 获取关注列表
     *
     * @param loginUser 当前登录用户
     * @return 关注列表
     */
    @Override
    public List<User> getFollowers(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        long loginUserId = loginUser.getId();
        QueryWrapper<Relationship> followRelationshipQueryWrapper = new QueryWrapper<>();
        followRelationshipQueryWrapper.eq("followerId", loginUserId);
        List<Relationship> followRelationships = this.list(followRelationshipQueryWrapper);

        if (followRelationships == null || followRelationships.isEmpty()) {
            throw new BusinessException(ErrorCode.NULL_DATA, "当前用户没有关注其他人");
        }
        // 获取关注用户id列表
        List<Long> followerIds = followRelationships.stream()
                .map(Relationship::getFollowedId)
                .collect(Collectors.toList());

        // 根据id获取关注用户集合
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", followerIds);
        List<User> followers = userService.list(userQueryWrapper);
        List<User> safetyFollowers = new ArrayList<>();

        // 脱敏返回
        for (User follower : followers) {
            User safetyUser = userService.getSafetyUser(follower);
            safetyFollowers.add(safetyUser);
        }
        return safetyFollowers;
    }

    /**
     * 管理员默认关注新用户
     *
     * @param userId 新用户id
     */
    @Override
    public void setDefaultFans(long userId) {
        Relationship followRelationship = new Relationship();
        followRelationship.setFollowerId(1L);
        followRelationship.setFollowedId(userId);
        boolean saveResult = this.save(followRelationship);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "默认关注失败");
        }
    }

}



