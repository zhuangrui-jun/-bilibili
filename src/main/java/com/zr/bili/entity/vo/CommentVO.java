package com.zr.bili.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论返回对象（B站式扁平化结构）
 */
@Data
public class CommentVO {
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long refId;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    
    /**
     * 被回复的评论ID（用于显示"回复 @用户名"）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToCommentId;
    
    /**
     * 被回复的用户信息（用于显示"回复 @用户名"）
     */
    private CommentUserVO replyToUser;
    
    private String content;
    
    /**
     * 评论者信息
     */
    private CommentUserVO user;
    
    /**
     * 点赞数（可选，如果Comment表有该字段）
     */
    private Integer likeCount;
    
    /**
     * 回复数（可选，如果Comment表有该字段）
     */
    private Integer replyCount;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * 子评论列表（扁平化：所有回复都作为一级评论的直接子评论）
     * B站方式：不管回复多少级，都扁平化显示
     */
    private List<CommentVO> children;
}

