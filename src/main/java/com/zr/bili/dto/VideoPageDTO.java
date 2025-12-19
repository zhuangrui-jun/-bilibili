package com.zr.bili.dto;

import lombok.Data;

@Data
public class VideoPageDTO {
    private Long creatorId;

    private int page;

    private int pageSize;
}
