package com.zr.bili.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DanmakuResponse<T> implements Serializable {
        private Integer code; // 0 表示成功（符合 DPlayer 约定）
        private T data;
        private String message;
    }