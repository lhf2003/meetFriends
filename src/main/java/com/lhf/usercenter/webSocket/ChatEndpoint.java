package com.lhf.usercenter.webSocket;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.exception.BusinessException;
import com.lhf.usercenter.config.GetHttpSessionConfig;
import com.lhf.usercenter.common.contant.UserConstant;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.ChatMessagesService;
import com.lhf.usercenter.service.impl.ChatMessagesServiceImpl;
import com.lhf.usercenter.webSocket.pojo.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.lhf.usercenter.common.contant.UserConstant.CHAT_WITH_AI;

@ServerEndpoint(value = "/chat", configurator = GetHttpSessionConfig.class)
@Component
@Slf4j
public class ChatEndpoint {

    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    // 存储当前聊天的用户组，用于标识已读消息
    private static final List<Map<Long, Long>> chatIds = new ArrayList<>();

    // 存储消息缓存key
    public static Set<String> chatUserKeys = new HashSet<>();

    private HttpSession httpSession;

    private static RedisTemplate redisTemplate;

    @Autowired
    public void setYourService(RedisTemplate redisTemplate) {
        ChatEndpoint.redisTemplate = redisTemplate;
    }

    // 静态方法获取Bean的示例
    private static RedisTemplate getRedisTemplate() {
        if (redisTemplate == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(ChatEndpoint.class);
        }
        return redisTemplate;
    }

    private static ChatMessagesService chatMessagesService;

    @Autowired
    public void setChatMessagesService(ChatMessagesService chatMessagesService) {
        ChatEndpoint.chatMessagesService = chatMessagesService;
    }

    // 静态方法获取Bean的示例
    private static ChatMessagesService getChatMessagesService() {
        if (chatMessagesService == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(ChatEndpoint.class);
        }
        return chatMessagesService;
    }

    /**
     * 连接成功时触发
     *
     * @param session
     * @param config
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 存储会话信息
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        User user = (User) this.httpSession.getAttribute(UserConstant.USER_LOGIN_STATUS);
        ChatMessagesServiceImpl.loginUser = user;
        long userId = user.getId();
        onlineUsers.put(userId, session);
        chatMessagesService = getChatMessagesService();

        // 标识消息已读
        boolean updateResult = chatMessagesService.updateUnReadMsg(chatIds);
        if (updateResult) chatIds.clear();
    }

    /**
     * 收到消息时触发
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        Gson gson = new Gson();
        try {
            // 将收到的字符串消息转换为 ResultMessage 对象
            ResultMessage msg = gson.fromJson(message, ResultMessage.class);

            long receiverId = msg.getReceiverId(); // 接收者id
            long senderId = msg.getSenderId(); // 发送者id

            // 记录当前聊天用户
            String key = "chat_messages:senderId:" + senderId + ":receiverId:" + receiverId;
            chatUserKeys.add(key);

            // 存储当前聊天的用户组，用于标识已读消息
            Map<Long, Long> chatId = new HashMap<>();
            chatId.put(senderId, receiverId);
            chatIds.add(chatId);


            // 创建一个新的 ResultMessage 对象，并将 msg 的属性复制给 resultMsg
            ResultMessage resultMsg = new ResultMessage();
            BeanUtils.copyProperties(msg, resultMsg);

            // 从在线用户列表中获取接收者的 WebSocket 会话
            Session session1 = onlineUsers.get(receiverId);
            Session session2 = onlineUsers.get(senderId);
            // 添加大模型处理逻辑
            if (receiverId == 0L) { // 判断是否是发给AI助手
                // 存储用户信息
                redisTemplate.opsForList().rightPush(key, msg);
                // 维护一个用户和AI的会话列表，用于连续对话
                Object object = redisTemplate.opsForValue().get(CHAT_WITH_AI + senderId);
                JSONArray jsonArray = object == null ? new JSONArray() : JSON.parseArray((String) object);
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", msg.getMessage());
                jsonArray.add(userMessage);
                String userMsgString = jsonArray.toJSONString();
//                log.info("用户提问："+userMsgString);
                redisTemplate.opsForValue().set(CHAT_WITH_AI + senderId, userMsgString);
                // 调用AI接口
                handleAIMessage(senderId);
                return;
            }
            boolean flag = true;
            // 对方用户也在线
            if (session1 != null && session2 != null) {
                String resultMessage = gson.toJson(resultMsg);
                // 通过 WebSocket 发送消息给接收者
                session1.getBasicRemote().sendText(resultMessage);
                msg.setReadStatus(1); // 连接中对话的消息为已读
                flag = false;
            }
            if (flag) {
                msg.setReadStatus(0); // 连接中对话的消息为未读
            }
            // 将消息存入 Redis 缓存
            redisTemplate.opsForList().rightPush(key, msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // AI消息处理方法
    private void handleAIMessage(Long senderId) {
        new Thread(() -> { // 使用新线程避免阻塞WebSocket
            try {
                // 调用DeepSeek接口
                JSONObject aiResponse = callDeepSeekAPI(senderId);

                // 构造AI回复消息
                ResultMessage aiMsg = new ResultMessage();
                aiMsg.setSenderId(0L); // AI用户ID
                aiMsg.setReceiverId(senderId);
                aiMsg.setMessage(aiResponse.getString("content"));
                aiMsg.setTimestamp(new Date());
                aiMsg.setReadStatus(1);

                // 发送给前端
                Session userSession = onlineUsers.get(senderId);
                if (userSession != null && userSession.isOpen()) {
                    userSession.getBasicRemote().sendText(new Gson().toJson(aiMsg));
                }

                // 存储消息
                String key = "chat_messages:senderId:0:receiverId:" + senderId;
                redisTemplate.opsForList().rightPush(key, aiMsg);

                // 维护一个用户和AI的会话列表，用于连续对话
                JSONArray chatHistory = JSON.parseArray((String) redisTemplate.opsForValue().get(CHAT_WITH_AI + senderId));
                if (chatHistory != null) {
                    chatHistory.add(aiResponse);
                } else {
                    log.error("chatHistory为空");
                }
                String aiMsgString = chatHistory.toJSONString();
//                log.info("AI回答："+aiMsgString);
                redisTemplate.opsForValue().set(CHAT_WITH_AI + senderId, aiMsgString);

            } catch (Exception e) {
                log.error("AI处理失败：", e);
            }
        }).start();
    }

    private JSONObject callDeepSeekAPI(Long senderId) {
        JSONArray messages = JSON.parseArray((String) redisTemplate.opsForValue().get(CHAT_WITH_AI + senderId));
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-ai/DeepSeek-V3");
        requestBody.put("messages", messages);
//        log.info("发起提问：" + requestBody.toJSONString());

        HttpRequest request = HttpRequest.post("https://api.siliconflow.cn/v1/chat/completions")
                .header("Authorization", "Bearer sk-iizujlskzfuirjzyxwtoaeenisgnvbtmagdjeaimqmhwfjrr")
                .header("Content-Type", "application/json")
                .body(requestBody.toJSONString());

        try (HttpResponse response = request.execute()) {
            // 解析响应体.获取回复内容
            String body = response.body();
            JSONObject responseBody = JSON.parseObject(body);
            JSONArray respMessage = (JSONArray) responseBody.get("choices");
            JSONObject object = (JSONObject) respMessage.get(0);
            return (JSONObject) object.get("message");
        } catch (Exception e) {
            log.error("请求错误：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用AI模型接口失败");
        }
    }

    /**
     * 连接关闭时触发
     *
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        // 移除登录态
        User user = (User) this.httpSession.getAttribute(UserConstant.USER_LOGIN_STATUS);
        long userId = user.getId();
        onlineUsers.remove(userId);
    }

    /**
     * 发生错误时触发
     *
     * @param session
     * @param throwable
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }


}