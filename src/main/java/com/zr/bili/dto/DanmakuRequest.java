package com.zr.bili.dto;

import lombok.Data;

/**
 * 前端发送弹幕的请求体.
 */
@Data
public class DanmakuRequest {

    /**
     * 弹幕池 / 视频标识，对应前端 danmaku.id.
     */
    private String id;

    /**
     * 发送者.
     */
    private String author;

    /**
     * 弹幕出现时间（秒）.
     */
    private Double time;

    /**
     * 弹幕内容.
     */
    private String text;

    /**
     * 颜色（十进制 RGB）.
     */
    private Integer color;

    /**
     * 弹幕类型：0 滚动，1 顶部，2 底部.
     */
    private Integer type;
}




























