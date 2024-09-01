package com.lhf.usercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhf.usercenter.model.domain.ChatMessages;

import java.util.List;
import java.util.Map;

/**
* @author LHF
* @description 针对表【chat_messages(用户私聊消息表)】的数据库操作Mapper
* @createDate 2024-08-18 11:50:07
* @Entity com.ziye.yupao.model.domain.ChatMessages
*/
public interface ChatMessagesMapper extends BaseMapper<ChatMessages> {

    /**
     * 更新消息已读状态
     * @param chatIds
     */
    void updateReadStatus(List<Map<Long, Long>> chatIds);

}



