package com.lhf.usercenter;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
@Slf4j
public class DeepSeekTest {
    public final static List<String> dialog = new ArrayList<>();
    public final static JSONArray HISTORY_MESSAGE = new JSONArray();
    public final static Scanner scanner = new Scanner(System.in);

    @Test
    public void test() {
        System.out.println("输入0退出，输入1开始或继续对话：");
        boolean flag = true;
        while (flag) {
            int userChoice = scanner.nextInt();
            switch (userChoice) {
                case 0:
                    flag = false;
                    System.out.println("退出聊天");
                    break;
                case 1:
                    beginChat();
                    break;
                default:
                    System.out.println("输入错误，请重新输入");
                    break;
            }
        }
    }

    private void beginChat() {
        String input = scanner.nextLine();
        dialog.add(input);
        // 构建用户输入消息对象
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", input);
        HISTORY_MESSAGE.add(message);
        try (HttpResponse response = sendMessage()) {
            // 解析响应体.获取回复内容
            String body = response.body();
            JSONObject responseBody = JSON.parseObject(body);
            JSONArray respMessage = (JSONArray) responseBody.get("choices");
            JSONObject object = (JSONObject) respMessage.get(0);
            JSONObject messageObj = (JSONObject) object.get("message");
            String content = messageObj.getString("content");
            // 构建回复消息对象
            JSONObject replyMessage = new JSONObject();
            replyMessage.put("role", "assistant");
            replyMessage.put("content", content);
            HISTORY_MESSAGE.add(replyMessage);
            dialog.add(content);
            System.out.println("回答:" + content);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private HttpResponse sendMessage() {
        // 使用 JSONObject 构建 JSON 请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-ai/DeepSeek-V3");
        requestBody.put("messages", HISTORY_MESSAGE);
        HttpRequest request = HttpRequest.post("https://api.siliconflow.cn/v1/chat/completions")
                .header("Authorization", "Bearer sk-iizujlskzfuirjzyxwtoaeenisgnvbtmagdjeaimqmhwfjrr")
                .header("Content-Type", "application/json")
                .body(requestBody.toJSONString());
        HttpResponse response = null;
        try {
            response = request.execute();
            int status = response.getStatus();
            if (status != 200) {
                log.error("请求错误：{}", response.body());
                throw new RuntimeException("请求错误", new Exception(response.body()));
            }
        } catch (Exception e) {
            log.error("请求失败：{}", e.getMessage());
        }
        return response;
    }
}