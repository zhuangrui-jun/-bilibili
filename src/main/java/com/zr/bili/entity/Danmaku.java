package com.zr.bili.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 弹幕实体.
 */
@Data
@TableName("danmaku")
public class Danmaku {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 弹幕池 / 视频标识，对应前端 danmaku.id.
     */
    @TableField("video_id")
    private String videoId;

    /**
     * 发送者id.
     */
    private Long authorId;

    /**
     * 弹幕出现时间（秒）.
     */
    private Double time;

    /**
     * 弹幕颜色（十进制 RGB）.
     */
    private Integer color;

    /**
     * 弹幕类型：0 滚动，1 顶部，2 底部.
     */
    private Integer type;

    /**
     * 弹幕内容.
     */
    private String text;

    /**
     * 创建时间.
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}































