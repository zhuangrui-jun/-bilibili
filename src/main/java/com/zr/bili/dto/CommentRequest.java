package com.zr.bili.dto;

import lombok.Data;

/**
 * 发送评论/回复评论的请求DTO
 */
@Data
public class CommentRequest {
    
    /**
     * 关联的实体ID（如视频ID）
     */
    private Long refId;
    
    /**
     * 父评论ID
     * 如果为0或null，表示这是一级评论
     * 如果不为0，表示这是回复评论
     */
    private Long parentId;
    
    /**
     * 被回复的评论ID（可选）
     * 用于显示"回复 @用户名"
     * 如果为空，则使用parentId对应的评论
     */
    private Long replyToCommentId;
    
    /**
     * 评论内容
     */
    private String content;
}





