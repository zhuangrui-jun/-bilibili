package com.zr.bili.rabbitmq;

import com.zr.bili.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatMessageConsumer {
    private final ChatWebSocketHandler chatWebSocketHandler;

    @RabbitListener(
            bindings = @QueueBinding(
                    // 1. 声明固定队列（接收所有离线消息）
                    value = @Queue(
                            name = "chat_offline_queue",
                            durable = "true",    // 队列持久化（离线消息不丢）
                            exclusive = "false", // 非排他（多实例可消费）
                            autoDelete = "false" // 不自动删除
                    ),
                    // 2. 声明TOPIC交换机（支持通配符路由键）
                    exchange = @Exchange(
                            name = "chat_exchange",
                            type = ExchangeTypes.TOPIC,
                            durable = "true"
                    ),
                    // 3. 使用通配符路由键匹配所有用户的消息（路由键格式：chat.user.{userId}）
                    key = "chat.user.*"
            )
    )
    public void consumeOfflineMessage(
            @Payload String msgJson,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws IOException {
        // 从路由键中提取用户ID（路由键格式：chat.user.{userId}）
        String toUserId = routingKey.substring("chat.user.".length());
        
        // 获取用户在线连接
        WebSocketSession session = chatWebSocketHandler.getOnlineUserSession(toUserId);
        if (session != null && session.isOpen()) {
            // 推送离线消息给前端
            session.sendMessage(new TextMessage(msgJson));
            log.info("消费离线消息：用户[{}]，内容：{}", toUserId, msgJson);
        } else {
            log.debug("用户[{}]不在线，消息已存入队列", toUserId);
        }
    }

}
