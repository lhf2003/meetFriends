package com.lhf.usercenter.controller;


import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.ResultUtil;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.vo.ChatMessagesVO;
import com.lhf.usercenter.service.ChatMessagesService;
import com.lhf.usercenter.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 聊天消息控制器类
 *
 * @author LHF
 */
@RestController
@RequestMapping("/chat")
public class ChatMessageController {

    @Resource
    private ChatMessagesService chatMessagesService;

    @Resource
    private UserService userService;

    /**
     * 获取历史聊天记录
     *
     * @param senderId 发送者id
     * @param request
     * @return
     */
    @GetMapping("/history/{senderId}")
    public BaseResponse<List<ChatMessagesVO>> getHistoryMessages(@PathVariable("senderId") long senderId,
                                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        long receiverId = loginUser.getId();
        List<ChatMessagesVO> historyMessages = chatMessagesService.getHistoryMessages(senderId, receiverId);
        return ResultUtil.success(historyMessages);
    }

}