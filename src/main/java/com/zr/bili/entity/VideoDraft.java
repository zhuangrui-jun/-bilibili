package com.zr.bili.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_draft")
public class VideoDraft {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String videoUrl;

    private String coverUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
