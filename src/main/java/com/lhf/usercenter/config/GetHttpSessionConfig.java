package com.lhf.usercenter.config;

import com.lhf.usercenter.common.contant.UserConstant;
import com.lhf.usercenter.model.domain.User;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class GetHttpSessionConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        //获取HttpSession对象
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        User attribute = (User) httpSession.getAttribute(UserConstant.USER_LOGIN_STATUS);
        System.out.println("建立连接的用户名：" + attribute.getUserName());
        //将httpSession对象保存起来
        sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
    }
}