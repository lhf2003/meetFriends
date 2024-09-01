package com.lhf.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.BaiduUtils;
import com.lhf.usercenter.common.utils.VerificationCodeUtil;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.model.domain.ReturnLocationBean;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.request.UserRegisterRequest;
import com.lhf.usercenter.service.UserOnlineStatusService;
import com.lhf.usercenter.service.UserService;
import com.lhf.usercenter.mapper.UserMapper;
import com.lhf.usercenter.common.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lhf.usercenter.contant.UserConstant.*;

/**
 * @author LHF
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2024-07-31 14:54:54
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserOnlineStatusService userOnlineStatusService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public static final String SALT = "lhf";

    @Override
    public boolean userRegister(UserRegisterRequest userRegisterRequest) {
        //校验参数是否合法
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if (StringUtils.isAllBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不匹配");
        }
        // 校验验证方式和验证码
        String registerMethod = userRegisterRequest.getRegisterMethod();
        if (StringUtils.isBlank(registerMethod)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "注册方式不能为空");
        }
        String verifyCode = userRegisterRequest.getVerifyCode();
        boolean verifyResult = VerificationCodeUtil.verifyCode(registerMethod, verifyCode);
        if (StringUtils.isBlank(verifyCode) || !verifyResult) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误");
        }

        // 封装用户信息
        User user = new User();
        if (registerMethod.contains("@")) {
            user.setEmail(registerMethod);
        } else {
            user.setPhone(registerMethod);
        }
        user.setUserAccount(userAccount);
        //加密密码
        String handledPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        user.setUserPassword(handledPassword);

        //用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount); //查询条件
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            log.info("userAccount already exist");
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }

        //插入数据库
        boolean result = this.save(user);
        if (!result) {
            log.info("userRegister fail");
            throw new BusinessException(ErrorCode.ERROR, "注册失败");
        }
        return result;
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验参数是否合法
        if (StringUtils.isAllBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度过短");
        }
        //封装用户信息
        User user = new User();
        user.setUserAccount(userAccount);
        //加密密码
        String handledPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        user.setUserPassword(handledPassword);

        //用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount); //查询条件
        queryWrapper.eq("userPassword", handledPassword); //查询条件
        user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("userAccount or userPassword is wrong");
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "账号或密码错误");
        }
        //脱敏用户信息
        User safelyUser = getSafetyUser(user);

        //将用户信息存入session
        request.getSession().setAttribute(USER_LOGIN_STATUS, safelyUser);
        // 设置缓存
        Object object = redisTemplate.opsForValue().get(USER_LOGIN_STATUS + safelyUser.getId());
        if (object == null) {
            redisTemplate.opsForValue().set(USER_LOGIN_STATUS + safelyUser.getId(), safelyUser, 3600 * 24, TimeUnit.SECONDS);
            String address = safelyUser.getAddress();
            if (StringUtils.isNotBlank(address)) {
                ReturnLocationBean locationBean = BaiduUtils.addressToLongitude(address);
                if (locationBean == null) {
                    throw new BusinessException(ErrorCode.ERROR, "地址解析失败");
                }
                log.info("为当前用户{}添加地址缓存", safelyUser.getId());
                redisTemplate.opsForGeo().add(USER_LOCATION_KEY, new Point(locationBean.getLng(), locationBean.getLat()), USER_LOGIN_STATUS + safelyUser.getId().toString());
            }
        }
        // 更新用户在线状态
        userOnlineStatusService.setUserStatus(user.getId(), 1);

        return safelyUser;
    }

    /**
     * 根据用户账号名查询用户信息
     *
     * @param userAccount 用户账号
     * @param request     请求
     * @return 用户信息
     */
    @Override
    public User searchUserByUserName(String userAccount, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("userAccount", userAccount);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 删除用户
     *
     * @param id      用户id
     * @param request 请求
     * @return 是否删除成功
     */
    @Override
    public boolean deleteUser(Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
        int result = userMapper.deleteById(id);
        return result > 0;

    }

    /**
     * 用户退出登录
     *
     * @param request 请求
     * @return 是否退出成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        userOnlineStatusService.setUserStatus(loginUser.getId(), 0);
        request.getSession().removeAttribute(USER_LOGIN_STATUS);
        return true;
    }

    /**
     * 判断用户是否为管理员
     *
     * @param request 请求
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        User user = (User) userObj;
        return Objects.equals(user.getUserRole(), USER_ADMIN);
    }

    @Override
    public boolean isAdmin(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        return loginUser.getUserRole().equals(USER_ADMIN);
    }

    /**
     * 脱敏用户信息
     *
     * @param user
     * @return
     */
    public User getSafetyUser(User user) {
        //脱敏用户信息
        User safelyUser = new User();
        safelyUser.setId(user.getId());
        safelyUser.setUserName(user.getUserName());
        safelyUser.setUserAccount(user.getUserAccount());
        safelyUser.setAge(user.getAge());
        safelyUser.setSex(user.getSex());
        safelyUser.setPhone(user.getPhone());
        safelyUser.setUserProfile(user.getUserProfile());
        safelyUser.setEmail(user.getEmail());
        safelyUser.setUserAvatar(user.getUserAvatar());
        safelyUser.setUserRole(user.getUserRole());
        safelyUser.setCreateTime(user.getCreateTime());
        safelyUser.setUserRole(user.getUserRole());
        safelyUser.setTags(user.getTags());
        safelyUser.setAddress(user.getAddress());
        safelyUser.setDistance(user.getDistance());

        return safelyUser;
    }

    /**
     * 获取当前用户信息
     *
     * @param request
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATUS);
        User user = (User) attribute;
        Object obj = redisTemplate.opsForValue().get(USER_LOGIN_STATUS + user.getId());
        if (obj != null) {
            return (User) obj;
        }
        return user;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
        return (User) userObj;
    }

    @Override
    public boolean updateUser(User user, User loginUser) {
        // 1、判断要修改的用户是否存在
        Long userId = user.getId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 2、开始修改用户信息
        // 判断user除id外其余属性是否都为空
        User validUser = new User();
        BeanUtils.copyProperties(user, validUser);
        validUser.setId(null);
        if (ObjectUtils.allNull(validUser)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 管理员或者当前登录用户才能修改用户信息
        if (!isAdmin(loginUser) && loginUser.getId() != userId) {
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
        // 3、修改用户信息
        boolean updateState = this.updateById(user);
        if (updateState) {
            log.info("修改用户{}信息成功", userId);
        } else {
            log.error("修改用户{}信息失败", userId);
            return false;
        }
        User newUser = userMapper.selectById(userId);
        // 更新缓存
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(USER_LOGIN_STATUS + loginUser.getId(), newUser, 24, TimeUnit.HOURS);
        // 地址缓存
        GeoOperations<String, Object> geo = redisTemplate.opsForGeo();
        ReturnLocationBean locationBean = BaiduUtils.addressToLongitude(user.getAddress());
        if (locationBean == null) {
            throw new BusinessException(ErrorCode.ERROR, "地址解析失败");
        }
        geo.add(USER_LOCATION_KEY, new Point(locationBean.getLng(), locationBean.getLat()), USER_LOGIN_STATUS + loginUser.getId());
        return true;
    }

    @Override
    public Page<User> recommendUsers(long pageNum, long pageSize, HttpServletRequest request) {
        // 1、获得当前登录用户
        User loginUser = getLoginUser(request);
        //指定缓存在redis中的路径
        String key = RECOMMEND_CACHE_KEY_PREFIX + loginUser.getId();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>) ops.get(key);
        List<User> userList;

        // 2、有缓存直接返回不需要查数据库
        if (userPage != null) {
            userList = userPage.getRecords();
            List<User> safetyUserList = userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
            userPage.setRecords(safetyUserList);
            return userPage;
        }

        // 3、没有缓存需要查数据库，并且将结果存入缓存
        // 3.1 查数据库，获取当前页数据
        userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), new QueryWrapper<>());
        userList = userPage.getRecords();
        // 计算其它用户与当前用户的距离
        ReturnLocationBean locationBean;
        for (int i = userList.size() - 1; i >= 0; i--) {
            User user = userList.get(i);
            // 不在页面展示自己的数据
            if (Objects.equals(user.getId(), loginUser.getId())) {
                userList.remove(i);
                continue;
            }
            String address = user.getAddress();
            locationBean = BaiduUtils.addressToLongitude(address);
            if (locationBean == null) {
                user.setDistance(0.0);
                continue;
            }
            GeoOperations<String, Object> geo = redisTemplate.opsForGeo();
            List<Point> position = geo.position(USER_LOCATION_KEY, USER_LOGIN_STATUS + user.getId());
            if (position == null || position.get(0) == null) {
                geo.add(USER_LOCATION_KEY, new Point(locationBean.getLng(), locationBean.getLat()), USER_LOGIN_STATUS + user.getId());
            }
            Distance distance = geo.distance(USER_LOCATION_KEY, USER_LOGIN_STATUS + loginUser.getId(), USER_LOGIN_STATUS + user.getId());
            Double userDistance = Optional.ofNullable(distance).map(Distance::getValue).orElse(0.0);
            user.setDistance(userDistance);
        }
        // 脱敏
        List<User> safetyUserList = userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
        userPage.setRecords(safetyUserList);
        // 3.2 存入缓存,一定要指定过期时间
        ops.set(key, userPage, 10, TimeUnit.HOURS);
        return userPage;
    }

    /**
     * 推荐匹配用户
     *
     * @param num       匹配用户数量
     * @param loginUser 当前登录用户
     * @return 匹配用户列表
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 1、获取所有有标签的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags");
        queryWrapper.select("id", "tags");
        List<User> userList = this.list(queryWrapper);

        // 2、获取当前登录用户的标签列表
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> currentUserTagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 存放用户相似度的列表（用户:相似度）
        List<Pair<User, Long>> list = new ArrayList<>();
        // 3、计算当前登录用户与所有有标签用户的相似度，并排序
        for (User user : userList) {
            String userTags = user.getTags();
            // 如果当前用户没有标签或者当前用户是登录用户，则跳过
            if (StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算编辑距离（值越小，相似度越高）
            long distance = AlgorithmUtils.minDistance(currentUserTagList, userTagList);
            // 将用户和相似度存入列表
            list.add(new Pair<>(user, distance));
        }
        //按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 4、返回用户列表
        // 获取用户id列表
        List<Long> userListVo = topUserPairList.stream().map(pari -> pari.getKey().getId()).collect(Collectors.toList());

        // 根据用户id列表查询用户脱敏后的信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userListVo);
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper).stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));

        // 因为在脱敏查询中使用in打乱了顺序，所以需要根据上面有序的用户id列表重新赋值排序
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userListVo) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }


    @Override
    public List<User> searchUserByTagsName(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 1、第一种方式使用SQL查询
/*
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            queryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
*/
        // 2、从内存中查
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<User> userList = (List<User>) ops.get(ALL_USER_CACHE_KEY);
        // 内存中没有数据，从数据库中查询并存到redis中，方便下次查询
        if (userList == null) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("tags");
            queryWrapper.select("id", "tags");
            userList = userMapper.selectList(queryWrapper);
            ops.set(ALL_USER_CACHE_KEY, userList, 24, TimeUnit.HOURS); // 存入redis，超时时间24小时
        }

        List<User> safetyUserList = new ArrayList<>();
        Gson gson = new Gson();
        // 遍历用户列表，校验是否匹配tagNameList
        for (User user : userList) {
            boolean flag = true;
            String userTags = user.getTags();
            Set<String> tagNameSet = gson.fromJson(userTags, new TypeToken<Set<String>>() {
            }.getType());
            tagNameSet = Optional.ofNullable(tagNameSet).orElse(new HashSet<>());
            for (String temp : tagNameList) {
                if (!tagNameSet.contains(temp)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                safetyUserList.add(user);
            }
        }
        return safetyUserList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }
}




