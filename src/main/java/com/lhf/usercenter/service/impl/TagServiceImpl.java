package com.lhf.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhf.usercenter.model.domain.Tag;
import com.lhf.usercenter.service.TagService;
import com.lhf.usercenter.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author LHF
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-08-04 11:48:36
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




