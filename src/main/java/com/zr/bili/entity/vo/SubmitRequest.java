package com.zr.bili.entity.vo;

import lombok.Data;

@Data
    public  class SubmitRequest {
        private Long draftId; // Spring会自动将前端传来的字符串转换为Long
        private String title;
        private String videoUrl;
        private String coverUrl;
    }