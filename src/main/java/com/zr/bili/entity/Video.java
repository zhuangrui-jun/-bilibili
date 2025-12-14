package com.zr.bili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video")
public class Video {

    private Long id;

    private String title;

    private String coverUrl;

    private Long creatorId;

    private Integer isHot;

    private Long times;

    //1080p视频地址
    private String videoUrl1;

    //720p视频地址
    private String videoUrl7;
    //480p视频地址
    private String videoUrl4;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
