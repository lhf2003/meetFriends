package com.lhf.usercenter.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.contant.UserContant;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.mapper.TeamMapper;
import com.lhf.usercenter.model.TeamQuery;
import com.lhf.usercenter.model.domain.Team;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.domain.UserTeam;
import com.lhf.usercenter.model.enums.TeamStatusEnum;
import com.lhf.usercenter.model.request.TeamJoinRequest;
import com.lhf.usercenter.model.request.TeamQuitRequest;
import com.lhf.usercenter.model.request.TeamRequest;
import com.lhf.usercenter.model.request.TeamUpdateRequest;
import com.lhf.usercenter.model.vo.TeamUserVO;
import com.lhf.usercenter.model.vo.UserVO;
import com.lhf.usercenter.service.TeamService;
import com.lhf.usercenter.service.UserService;
import com.lhf.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author LHF
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private TeamMapper teamMapper;
    @Resource
    private UserService userService;
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional
    public long addTeam(TeamRequest teamRequest, HttpServletRequest request) {
        // 1、请求参数是否为空
        if (teamRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 2、获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        final Long userId = loginUser.getId();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        // 3、校验队伍信息
        // 3、1 队伍人数是否合法
        int maxNum = Optional.ofNullable(teamRequest.getMaxNum()).orElse(0);//为空默认为0
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍人数错误");
        }
        //  3、2 校验队伍名称
        if (StringUtils.isBlank(teamRequest.getName()) ||
                ((teamRequest.getName().length() < 4) || (teamRequest.getName().length() > 20))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍名称错误");
        }
        // 3、3 校验队伍描述
        if (StringUtils.isBlank(teamRequest.getDescription()) || teamRequest.getDescription().length() > 512) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍描述错误");
        }

        // 3、4 校验队伍是否过期
        Date expireTime = teamRequest.getExpireTime();
        if (expireTime == null || new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍已过期");
        }
        // 3、5 校验队伍状态，status 是否公开，不传默认为0
        int status = Optional.ofNullable(teamRequest.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍状态不满足要求");
        }
        // 3、6 如果status是加密状态，一定要密码 且密码<=32
        String password = teamRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum) && (StringUtils.isBlank(password) || password.length() > 32)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码设置不正确");
        }

        // 3、7 校验用户最多创建5个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long count = teamMapper.selectCount(queryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最多创建5个队伍");
        }

        //插入队伍表
        Team team = new Team();
        BeanUtil.copyProperties(teamRequest, team);
        team.setUserId(userId); //设置创建用户id
        boolean save = this.save(team);
        Long teamId = team.getId();
        if (!save || teamId == null) {
            throw new BusinessException(ErrorCode.ERROR, "插入队伍失败");
        }
        //插入用户队伍表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        boolean result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.ERROR, "插入用户队伍失败");
        }
        return teamId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(Long id, HttpServletRequest request) {
        // 校验：队伍是否存在、用户是否为队伍的队长或管理员。
        Team team = getTeamById(id);
        authJudge(team, request);
        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        Long teamId = team.getId();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "删除队伍关系失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest newTeam, HttpServletRequest request) {
        // 判断请求参数是否为空。
        if (newTeam == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 校验：队伍是否存在、用户是否为队伍的队长或管理员。
        Team oldTeam = teamMapper.selectById(newTeam.getId());
        authJudge(oldTeam, request);

        // 如果队伍状态改为加密，必须要有密码。
        if (newTeam.getStatus().equals(TeamStatusEnum.SECRET.getValue())) {
            if (newTeam.getPassword() == null || newTeam.getPassword().length() < 8) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "加密队伍必须要有密码");
            }
        }
        // 更新成功
        Team team = new Team();
        BeanUtil.copyProperties(newTeam, team);
        return this.updateById(team);
    }

    public boolean authJudge(Team team, HttpServletRequest request) {
        if (team == null || team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NULL_DATA, "队伍不存在");
        }
        // 校验用户是否为队伍的队长或管理员。
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null ||
                (!loginUser.getUserRole().equals(UserContant.USER_ADMIN) && !loginUser.getId().equals(team.getUserId()))) {
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
        return true;
    }

    /**
     * 判断是否为管理员
     *
     * @param request 请求
     * @return 是否为管理员
     */
    public boolean isAdmin(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return loginUser.getUserRole().equals(UserContant.USER_ADMIN);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        // 队伍必须存在，只能加入未满、未过期的队伍
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_DATA, "队伍不存在");
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        long count = userTeamService.count(queryWrapper);
        if (count >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍已满");
        }

        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍已过期");
        }

        // 禁止加入私有队伍
        Integer status = team.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "禁止加入私有队伍");
        }

        // 如果加入的队伍是加密的，必须密码要匹配才可以
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
            }
        }
        // 用户最多加入 5 个队伍
        long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        // userId等于userId，且idDelete为0
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("isDelete", 0);
        RLock lock = redissonClient.getLock("meetfriends:join_team:" + userId);
        // 返回标识符
        boolean save = false;
        try {
            if (lock.tryLock(1, 5, TimeUnit.SECONDS)) {
                long hasJoinTeam = userTeamService.count(userTeamQueryWrapper);
                if (hasJoinTeam >= 5) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "用户最多加入 5 个队伍");
                }
                // 用户不能重复加入已加入的队伍
                userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("userId", userId);
                userTeamQueryWrapper.eq("teamId", teamId);
                long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                if (hasUserJoinTeam > 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "不能重复加入队伍");
                }
                // 修改队伍信息
                UserTeam userTeam = new UserTeam();
                userTeam.setUserId(userId);
                userTeam.setTeamId(teamId);
                userTeam.setJoinTime(new Date());
                save = userTeamService.save(userTeam);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return save;
    }


    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            // 根据 id 查询
            Long id = teamQuery.getId();
            if (id != null) {
                queryWrapper.eq("id", id);
            }

            // 根据队伍名称查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }

            // 根据关键词查询（队伍名称和描述）
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }

            // 根据队伍描述查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }

            // 根据队伍最大人数查询
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
        }

        // 根据用户（队长）id 查询
        Long userId = teamQuery.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }

        // 根据队伍状态来查询
        Integer status = teamQuery.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            statusEnum = TeamStatusEnum.PUBLIC;
        }
//        if (!statusEnum.equals(TeamStatusEnum.PUBLIC)) {
//            throw new BusinessException(ErrorCode.AUTH_ERROR);
//        }
        queryWrapper.eq("status", statusEnum.getValue());

        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));

        List<Team> teamList = this.list(queryWrapper);
        if (teamList == null) {
            return new ArrayList<>();
        }

        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (Team team : teamList) {
            userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtil.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            User user = userService.getById(userId);
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtil.copyProperties(user, userVO);
                teamUserVO.setCreatedUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        // 2、判断当前用户是否已加入队伍
//        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
//        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
//        try {
//            User loginUser = userService.getLoginUser(request);
//            userTeamQueryWrapper.eq("userId", loginUser.getId());
//            userTeamQueryWrapper.in("teamId", teamIdList);
//            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
//            // 已加入的队伍 id 集合
//            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
//            teamList.forEach(team -> {
//                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
//                team.setHasJoin(hasJoin);
//            });
//        } catch (Exception e) {
//        }
        // 3、查询已加入队伍的人数
        List<Long> teamIdList = teamUserVOList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍 id => 加入这个队伍的用户列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserVOList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));

        return teamUserVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        // 校验请求参数
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 校验队伍是否存在
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_DATA, "队伍不存在");
        }
        // 校验当前登录用户是否在队伍
        long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setUserId(userId);
        queryUserTeam.setTeamId(teamId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未加入队伍");
        }
        long teamHasJoinNum = countTeamUser(teamId);
        // 队伍只剩一人,解散队伍
        if (teamHasJoinNum == 1) {
            // 删除队伍
            this.removeById(teamId);
        } else {// 队伍里不是只有一人
            // 当前登录用户是否是当前队伍的队长
            if (team.getUserId() == userId) {// 是队长
                // 把队伍转移给最早加入队伍的用户
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                // 只需要查出两条关系,因为只需要查出两个用户,一个是队长(即当前登录用户),另一个是最早加入队伍的用户
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.ERROR);
                }
                // 找到最早加入队伍用户的那条关系
                UserTeam nextUserTeam = userTeamList.get(1);
                // 获取最早加入用户的id,也就是下一任队长的id
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新为当前队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.ERROR, "更新队伍队长失败");
                }
            }
        }
        // 移除关系(这里是公共代码,包含了队伍只剩1人,当前用户是队长或不是队长三种情况)
        return userTeamService.remove(queryWrapper);
    }

    @Override
    public List<Team> getJoinTeamList(HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取当前用户加入的队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        List<Long> teamIdList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        return teamMapper.selectList(new QueryWrapper<Team>().in("id", teamIdList));
    }

    @Override
    public List<Team> getCreateTeamList(HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取当前用户创建的队伍
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", loginUser.getId());
        return teamMapper.selectList(teamQueryWrapper);
    }

    // 获取队伍当前的人数
    private long countTeamUser(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

    // 根据队伍id获取队伍
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.ERROR, "队伍不存在");
        }
        return team;
    }

}