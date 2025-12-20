package com.zr.bili.entity.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论中的用户信息
 */
@Data
public class CommentUserVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    private String username;
    
    private String avatar;

    private LocalDateTime createdTime;
}

