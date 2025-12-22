package com.zr.bili.handler;

import com.alibaba.fastjson.JSON;
import com.zr.bili.entity.ChatMessage;
import com.zr.bili.entity.vo.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {



    private static final Map<String ,WebSocketSession> onlineUsers=new ConcurrentHashMap<>();


    private final RabbitTemplate rabbitTemplate;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId=session.getUri().getQuery().split("=")[1];
        onlineUsers.put(userId,session);
        System.out.println("用户"+userId+"已连接,当前上线人数："+onlineUsers.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        ChatMessage msg= JSON.parseObject(textMessage.getPayload(), ChatMessage.class);
        String userId=msg.getUserId();
        String toUserId=msg.getToUserId();
        String content=msg.getContent();

        ChatResponse response=new ChatResponse(userId,content,System.currentTimeMillis());
        String responseJson=JSON.toJSONString(response);

        //判断目标用户是否在线，在线就直接发，不在线就进行rabbitmq
        WebSocketSession toSession=onlineUsers.get(toUserId);
        if (toSession!=null&& toSession.isOpen()){
            toSession.sendMessage(new TextMessage(responseJson));
        }else{
            // 使用TOPIC交换机的路由键格式：chat.user.{userId}
            rabbitTemplate.convertAndSend(
                    "chat_exchange",
                    "chat.user." + toUserId,
                    responseJson
            );
            System.out.println("用户"+toUserId+"不在线，消息已发送到RabbitMQ");
        }


    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        String userId=session.getUri().getQuery().split("=")[1];
        onlineUsers.remove(userId);
        System.out.println("用户"+userId+"已断开连接,当前在线人数："+onlineUsers.size());
    }

    public WebSocketSession getOnlineUserSession(String userId) {
        return onlineUsers.get(userId);
    }
}
