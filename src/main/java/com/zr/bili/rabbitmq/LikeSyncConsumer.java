package com.zr.bili.rabbitmq;

import com.zr.bili.service.VlikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeSyncConsumer {

    private final VlikeService vlikeService;
    
    // 队列名称（与生产者保持一致）
    private static final String LIKE_SYNC_QUEUE = "like_sync_queue";

    /**
     * 消费点赞同步消息，异步执行同步任务
     * @param message 消息内容（这里只是触发信号，实际内容不重要）
     */
    @RabbitListener(
        queuesToDeclare = @Queue(
            name = LIKE_SYNC_QUEUE,
            durable = "true"  // 队列持久化，服务重启后消息不丢失
        )
    )
    public void consumeLikeSync(String message) {
        log.info("收到点赞同步消息，开始异步执行同步任务");
        try {
            vlikeService.syncLikeDataToDatabase();
            log.info("点赞同步任务执行完成");
        } catch (Exception e) {
            log.error("异步执行点赞同步任务失败", e);
            // 这里可以根据需要实现重试机制或发送到死信队列
        }
    }
}

