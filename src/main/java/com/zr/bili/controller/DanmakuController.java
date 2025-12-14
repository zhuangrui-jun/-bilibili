package com.zr.bili.controller;

import com.zr.bili.dto.DanmakuRequest;
import com.zr.bili.result.DanmakuResponse;
import com.zr.bili.service.DanmakuService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * DPlayer 弹幕接口（兼容 DPlayer 约定的返回格式）。
 */
@RestController
@RequestMapping("/danmakuv3")
public class DanmakuController {

    private final DanmakuService danmakuService;

    public DanmakuController(DanmakuService danmakuService) {
        this.danmakuService = danmakuService;
    }

    /**
     * 获取弹幕列表：GET /danmaku/?id=xxx&max=3000
     * DPlayer 默认会调用：{api}/v3/?id={id}&max={max}
     * 因此兼容 / 和 /v3/ 两种路径。
     */
    @GetMapping({"/",""})
    public DanmakuResponse<List<List<Object>>> listDanmaku(
            @RequestParam("id") String videoId,
            @RequestParam(value = "max", required = false, defaultValue = "3000") Integer max) {

        List<List<Object>> data = danmakuService.listDanmaku(videoId, max);
        return new DanmakuResponse<>(0, data, null);
    }

    /**
     * 发送弹幕：POST /danmaku/
     * DPlayer 默认会调用：POST {api}/v3/
     * 因此兼容 / 和 /v3/ 两种路径。
     */
    @PostMapping({ "/", ""})
    public DanmakuResponse<List<Object>> sendDanmaku(@RequestBody DanmakuRequest request) {
        danmakuService.saveDanmaku(request);
        return new DanmakuResponse<>(0, Collections.emptyList(), null);
    }


}

