package com.lhf.usercenter.listener;

import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.model.domain.UserOnlineStatus;
import com.lhf.usercenter.service.UserOnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.session.Session;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.lhf.usercenter.common.contant.UserConstant.USER_LOGIN_STATUS;

@Component
@Slf4j
public class RedisSessionEventListener {
    @Autowired
    private UserOnlineStatusService userOnlineStatusService;

    @EventListener
    public void handleSessionDestroyed(SessionDestroyedEvent event) {
        log.info("session销毁事件");
        Session session = event.getSession();
        User user = session.getAttribute(USER_LOGIN_STATUS);
        session.removeAttribute(USER_LOGIN_STATUS);
        if(user == null) {
            log.error("无法获取用户信息");
            return;
        }
        UserOnlineStatus userOnlineStatus = new UserOnlineStatus();
        userOnlineStatus.setUserId(user.getId());
        userOnlineStatus.setIsOnline(0);
        userOnlineStatus.setLastOnline(new Date());
        try{
            userOnlineStatusService.updateById(userOnlineStatus);
        }catch(Exception e){
            log.error("更新用户在线状态失败", e);
        }
    }
}