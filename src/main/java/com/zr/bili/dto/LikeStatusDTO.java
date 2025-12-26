package com.zr.bili.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 点赞状态DTO
 */
@Data
public class LikeStatusDTO implements Serializable {
    /**
     * 是否已点赞
     */
    private Boolean isLiked;

    /**
     * 点赞数
     */
    private Integer likeCount;
}


