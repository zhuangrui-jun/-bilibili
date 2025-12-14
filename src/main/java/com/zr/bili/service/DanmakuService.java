package com.zr.bili.service;

import com.zr.bili.dto.DanmakuRequest;
import com.zr.bili.entity.Danmaku;

import java.util.List;

public interface DanmakuService {

    /**
     * 保存弹幕.
     */
    void saveDanmaku(DanmakuRequest request);

    /**
     * 查询弹幕，格式符合 DPlayer 需要的二维数组.
     */
    List<List<Object>> listDanmaku(String videoId, Integer max);

}


