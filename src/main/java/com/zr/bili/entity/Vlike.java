package com.zr.bili.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_likes")
public class Vlike {

    private Long id;

    private Long videoId;

    private Long userId;

    private LocalDateTime createdTime;

    private Boolean isLiked;
}
