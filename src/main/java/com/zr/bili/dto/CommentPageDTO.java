package com.zr.bili.dto;

import lombok.Data;

@Data
public class CommentPageDTO {
    private Long videoId;

    private int page;

    private int pageSize;
}
