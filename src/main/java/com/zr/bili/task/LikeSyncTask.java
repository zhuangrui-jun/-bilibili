package com.zr.bili.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeSyncTask {

    private final RabbitTemplate rabbitTemplate;
    
    // 队列名称
    private static final String LIKE_SYNC_QUEUE = "like_sync_queue";

    //30分钟的任务
    @Scheduled(cron = "0 */30 * * * ?")
    public void triggerLikeSync() {
        log.info("定时任务：发送点赞同步消息到RabbitMQ");
        try {
            // 发送消息到队列（异步执行）
            rabbitTemplate.convertAndSend(LIKE_SYNC_QUEUE, "sync");
            log.info("点赞同步消息已发送到队列：{}", LIKE_SYNC_QUEUE);
        } catch (Exception e) {
            log.error("发送点赞同步消息失败", e);
        }
    }
}


