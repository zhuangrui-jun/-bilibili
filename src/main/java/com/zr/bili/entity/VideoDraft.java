package com.zr.bili.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_draft")
public class VideoDraft {

    private Long id;

    private String title;

    private String VideoUrl;

    private Long creatorId;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
