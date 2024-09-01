package com.lhf.usercenter.controller;

import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.ResultUtil;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.vo.FriendVO;
import com.lhf.usercenter.service.RelationshipService;
import com.lhf.usercenter.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 好友列表控制器类
 */
@RestController
@RequestMapping("/friends")
public class FriendsController {

    @Resource
    private RelationshipService followRelationshipService;

    @Resource
    private UserService userService;

    /**
     * 获取所有好友（关注/粉丝）列表
     *
     * @param request
     * @return
     */
    @GetMapping("/all")
    public BaseResponse<List<FriendVO>> getAllFriends(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        List<FriendVO> friends = followRelationshipService.getAllFriends(loginUser);
        return ResultUtil.success(friends);
    }

    /**
     * 获取当前用户粉丝
     *
     * @param request
     * @return 粉丝集合
     */
    @GetMapping("/fans")
    public BaseResponse<List<User>> getFans(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        List<User> fans = followRelationshipService.getFans(loginUser);
        return ResultUtil.success(fans);
    }

    /**
     * 获取当前用户关注用户
     *
     * @param request
     * @return 粉丝集合
     */
    @GetMapping("/followers")
    public BaseResponse<List<User>> getFollowers(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        List<User> followers = followRelationshipService.getFollowers(loginUser);
        return ResultUtil.success(followers);
    }

}