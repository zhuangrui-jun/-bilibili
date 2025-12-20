package com.zr.bili.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long refId;

    private Long parentId;

    private String content;

    private Long creatorId;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
