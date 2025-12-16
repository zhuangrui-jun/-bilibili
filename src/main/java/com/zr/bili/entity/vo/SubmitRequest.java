package com.zr.bili.entity.vo;

import lombok.Data;

@Data
    public  class SubmitRequest {
        private Long draftId;
        private String title;
        private String videoUrl;
    }