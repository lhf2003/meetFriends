package com.lhf.usercenter.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.ResultUtil;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.model.TeamQuery;
import com.lhf.usercenter.model.domain.Team;
import com.lhf.usercenter.model.request.*;
import com.lhf.usercenter.model.vo.TeamUserVO;
import com.lhf.usercenter.service.TeamService;
import com.lhf.usercenter.service.UserService;
import com.lhf.usercenter.service.UserTeamService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/team")
public class TeamController {
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamRequest teamRequest, HttpServletRequest request) {
        if (teamRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        long result = teamService.addTeam(teamRequest, request);
        return ResultUtil.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        Long id = deleteRequest.getId();
        if (deleteRequest == null || id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = teamService.deleteTeam(id, request);
        return ResultUtil.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = teamService.updateTeam(teamUpdateRequest, request);
        if (!result) {
            throw new BusinessException(ErrorCode.ERROR, "更新失败");
        }
        return ResultUtil.success(result);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> getTeamList(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
//        boolean admin = teamService.isAdmin(request);
        // 1、查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery);
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
        return ResultUtil.success(teamList);
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<Team>> getTeamListByPage(@RequestBody TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Page<Team> teamPage = teamService.getTeamListByPage(teamQuery);

        return ResultUtil.success(teamPage);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team team = teamService.getById(id);
        return ResultUtil.success(team);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = teamService.joinTeam(teamJoinRequest, request);
        return ResultUtil.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = teamService.quitTeam(teamQuitRequest, request);
        return ResultUtil.success(result);
    }

    @GetMapping("list/join")
    public BaseResponse<List<TeamUserVO>> getJoinTeamList(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        List<TeamUserVO> joinTeamList = teamService.getJoinTeamList(request);
        return ResultUtil.success(joinTeamList);
    }

    @GetMapping("list/create")
    public BaseResponse<List<TeamUserVO>> getCreateTeamList(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        List<TeamUserVO> createTeamList = teamService.getCreateTeamList(request);
        return ResultUtil.success(createTeamList);
    }
}