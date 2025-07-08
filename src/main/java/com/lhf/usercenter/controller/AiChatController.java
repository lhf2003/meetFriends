package com.lhf.usercenter.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.ResultUtil;
import com.lhf.usercenter.model.request.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// SSE控制器
@RestController
@RequestMapping("/chat")
@Slf4j
public class AiChatController {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<Long, JSONArray> chatHistories = new ConcurrentHashMap<>();

    // 前端建立SSE连接
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam Long userId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        // 初始化对话历史
        chatHistories.putIfAbsent(userId, new JSONArray());

        return emitter;
    }

    // 处理用户消息
    @PostMapping("/send")
    public BaseResponse<?> sendMessage(@RequestBody MessageRequest message) {
        JSONArray history = chatHistories.get(message.getUserId());

        // 添加用户消息到历史
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", message.getContent());
        history.add(userMsg);

        // 异步处理AI响应
        CompletableFuture.runAsync(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "deepseek-ai/DeepSeek-V3");
                requestBody.put("messages", history);
                requestBody.put("stream", true); // 启用流式

                HttpRequest request = HttpRequest.post("https://api.siliconflow.cn/v1/chat/completions")
                        .header("Authorization", "Bearer " + System.getenv("DEEPSEEK_API_KEY"))
                        .header("Content-Type", "application/json")
                        .body(requestBody.toString());

                try (HttpResponse response = request.execute()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.bodyStream()));
                    String line;
                    StringBuilder contentBuffer = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String jsonStr = line.substring(6);
                            JSONObject data = JSON.parseObject(jsonStr);
                            String delta = data.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("delta")
                                    .getString("content");

                            if (delta != null) {
                                contentBuffer.append(delta);
                                // 实时推送每个delta
                                emitters.get(message.getUserId()).send(SseEmitter.event()
                                        .data(delta)
                                        .id(UUID.randomUUID().toString()));
                            }
                        }
                    }

                    // 保存完整响应到历史
                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", contentBuffer.toString());
                    history.add(assistantMsg);
                }
            } catch (Exception e) {
                log.error("API调用失败: {}", e.getMessage());
                emitters.get(message.getUserId()).completeWithError(e);
            }
        });

        return ResultUtil.success("");
    }
}