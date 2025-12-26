package com.zr.bili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video")
public class Video {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String coverUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    private Integer isHot;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long times;

    //1080p视频地址
    private String videoUrl1;

    //720p视频地址
    private String videoUrl7;
    //480p视频地址
    private String videoUrl4;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private String creatorName;

    private int likes;
}
