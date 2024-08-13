package com.lhf.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhf.usercenter.model.domain.UserTeam;
import com.lhf.usercenter.service.UserTeamService;
import com.lhf.usercenter.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author LHF
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-08-07 23:18:54
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




