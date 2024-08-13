package com.lhf.usercenter.service;

import com.lhf.usercenter.model.TeamQuery;
import com.lhf.usercenter.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lhf.usercenter.model.request.TeamJoinRequest;
import com.lhf.usercenter.model.request.TeamQuitRequest;
import com.lhf.usercenter.model.request.TeamRequest;
import com.lhf.usercenter.model.request.TeamUpdateRequest;
import com.lhf.usercenter.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author LHF
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param teamRequest team请求类
     * @param request     http请求
     * @return 队伍id
     */
    long addTeam(TeamRequest teamRequest, HttpServletRequest request);

    /**
     * 删除队伍
     *
     * @param id      队伍id
     * @param request http请求
     * @return 是否成功
     */
    boolean deleteTeam(Long id, HttpServletRequest request);

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest team更新请求类
     * @param request           http请求
     * @return 是否成功
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, HttpServletRequest request);

    /**
     * 查询队伍
     *
     * @param teamQuery team查询类
     * @return 队伍列表
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery);

    /**
     * 判断是否为管理员
     *
     * @param request http请求
     * @return 是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 加入队伍
     *
     * @param teamJoinRequest team加入请求类
     * @param request         http请求
     * @return 是否成功
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request);

    /**
     * 退出队伍
     *
     * @param teamQuitRequest team退出请求类
     * @param request         http请求
     * @return 是否成功
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request);

    /**
     * 获取当前用户加入队伍的数量
     *
     * @param request http请求
     * @return 加入队伍数量
     */
    List<Team> getJoinTeamList(HttpServletRequest request);

    /**
     * 获取当前用户创建队伍的数量
     *
     * @param request http请求
     * @return 创建队伍数量
     */
    List<Team> getCreateTeamList(HttpServletRequest request);
}
