package com.lhf.usercenter.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.MailUtils;
import com.lhf.usercenter.common.utils.ResultUtil;
import com.lhf.usercenter.common.utils.VerificationCodeUtil;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.request.UserLoginRequest;
import com.lhf.usercenter.model.request.UserModifyPasswordRequest;
import com.lhf.usercenter.model.request.UserRegisterRequest;
import com.lhf.usercenter.service.RelationshipService;
import com.lhf.usercenter.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.lhf.usercenter.contant.UserConstant.*;

@Api("用户模块")
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RelationshipService relationshipService;

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Boolean> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAllBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = userService.userRegister(userRegisterRequest);
        return ResultUtil.success(result);
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAllBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtil.success(user);
    }

    @ApiOperation("搜索用户")
    @GetMapping("/search")
    public BaseResponse<User> searchUser(@RequestParam("userName") String userName) {
        if (StringUtils.isBlank(userName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User user = userService.searchUser(userName);
        return ResultUtil.success(user);
    }

    @ApiOperation("删除用户")
    @PostMapping("/delete")
    public BaseResponse deleteUser(@RequestBody Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        boolean result = userService.deleteUser(id, request);
        return ResultUtil.success(result);
    }

    @ApiOperation("用户注销")
    @GetMapping("/logout")
    public BaseResponse userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        boolean result = userService.userLogout(request);
        return ResultUtil.success(result);
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        User user = (User) userObj;
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        // 校验
        User currentUser = userService.getCurrentUser(request);
        // 脱敏
        User safetyUser = userService.getSafetyUser(currentUser);
        return ResultUtil.success(safetyUser);
    }

    @ApiOperation("更改用户信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null || request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 校验
        User loginUser = userService.getLoginUser(request);
        // 更新
        boolean result = userService.updateUser(user, loginUser);
        return ResultUtil.success(result);
    }

    @ApiOperation("通过标签名搜索用户")
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTagsName(@RequestParam("tagNameList") List<String> tagNameList) {
        if (tagNameList == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        return ResultUtil.success(userService.searchUserByTagsName(tagNameList));
    }

    @ApiOperation("用户推荐")
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageNum, long pageSize, HttpServletRequest request) {
        if (pageNum <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        Page<User> page = userService.recommendUsers(pageNum, pageSize, request);
        //返回脱敏后的数据
        return ResultUtil.success(page);
    }

    @ApiOperation("用户匹配")
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<User> userList = userService.matchUsers(num, loginUser);
        return ResultUtil.success(userList);
    }

    @ApiOperation("根据id获取用户信息")
    @GetMapping("/get")
    public BaseResponse<User> getUserInfoById(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return ResultUtil.success(user);
    }

    @ApiOperation("生成注册验证码")
    @GetMapping("/get/verifyCode")
    public BaseResponse<String> generateCode(String registerMethod) {
        if (StringUtils.isBlank(registerMethod)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的验证方式");
        }
        // 生成验证码
        String code = VerificationCodeUtil.generateCode();
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.ERROR, "验证码生成失败");
        }
        // 存储验证码，method是用户选择的验证方式（邮箱/手机号）
        VerificationCodeUtil.storeCode(registerMethod, code);
        // 判断验证方式，如果是邮箱则发送邮件，如果是手机号则发送短信
        if (StringUtils.contains(registerMethod, "@")) {
            MailUtils.sendMail(registerMethod, code, USER_REGISTER);
        }
        // TODO 发送手机验证码
        return ResultUtil.success(code);
    }

    @ApiOperation("修改密码")
    @PostMapping("/modify/password")
    public BaseResponse<Integer> modifyPassword(@RequestBody UserModifyPasswordRequest userModifyPasswordRequest, HttpServletRequest httpServletRequest) {
        if (userModifyPasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String oldPassword = userModifyPasswordRequest.getOldPassword();
        String newPassword = userModifyPasswordRequest.getNewPassword();
        String checkPassword = userModifyPasswordRequest.getCheckPassword();

        if (StringUtils.isAllBlank(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        int result = userService.modifyPassword(oldPassword, newPassword, checkPassword, httpServletRequest);
        return ResultUtil.success(result);
    }

    @ApiOperation("找回密码")
    @GetMapping("/find/password")
    public BaseResponse<Integer> findPassword(@RequestParam("email") String email) {
        if (StringUtils.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        int result = userService.findPassword(email);
        return ResultUtil.success(result);
    }

    @ApiOperation("获取粉丝数")
    @GetMapping("/follows/{id}")
    public BaseResponse<Long> getUserFollowNum(@PathVariable Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        long followNum = relationshipService.getFollowNum(id);
        return ResultUtil.success(followNum);
    }

    @ApiOperation("获取关注数")
    @GetMapping("/fans/{id}")
    public BaseResponse<Long> getUserFansNum(@PathVariable Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        long fansNum = relationshipService.getFansNum(id);
        return ResultUtil.success(fansNum);
    }

    @ApiOperation("关注用户")
    @PostMapping("dofollow/{id}")
    public BaseResponse<String> doFollow(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = (User) httpServletRequest.getSession().getAttribute(USER_LOGIN_STATUS);
        String result = relationshipService.followUser(id, loginUser);
        return ResultUtil.success(result);
    }

    @ApiOperation("判断是否关注")
    @GetMapping("/isfans/{id}")
    public BaseResponse<Boolean> isFans(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = (User) httpServletRequest.getSession().getAttribute(USER_LOGIN_STATUS);
        boolean result = relationshipService.isFans(id, loginUser);
        return ResultUtil.success(result);
    }

    @ApiOperation("获取当前私聊用户对象")
    @GetMapping("/get/{id}")
    public BaseResponse<User> getCurrentChatUser(@PathVariable Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User user = userService.getById(id);
        return ResultUtil.success(user);
    }
}