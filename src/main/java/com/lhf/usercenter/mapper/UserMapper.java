package com.lhf.usercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhf.usercenter.model.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
* @author LHF
* @description 针对表【user】的数据库操作Mapper
* @createDate 2024-07-31 14:54:54
* @Entity com.lhf.usercenter.model.domain.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {
}




