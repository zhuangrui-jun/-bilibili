package com.zr.bili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.bili.context.BaseContext;
import com.zr.bili.dto.DanmakuRequest;
import com.zr.bili.entity.Danmaku;
import com.zr.bili.mapper.DanmakuMapper;
import com.zr.bili.service.DanmakuService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DanmakuServiceImpl extends ServiceImpl<DanmakuMapper, Danmaku> implements DanmakuService {

    @Override
    public void saveDanmaku(DanmakuRequest request) {
        Danmaku danmaku = new Danmaku();
        danmaku.setVideoId(request.getId());
        danmaku.setAuthorId(BaseContext.getCurrentId());
        danmaku.setTime(request.getTime());
        danmaku.setText(request.getText());
        danmaku.setColor(request.getColor());
        danmaku.setType(request.getType());
        danmaku.setCreateTime(LocalDateTime.now());
        this.save(danmaku);
    }

    @Override
    public List<List<Object>> listDanmaku(String videoId, Integer max) {
        LambdaQueryWrapper<Danmaku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(videoId), Danmaku::getVideoId, videoId)
                .orderByAsc(Danmaku::getTime)
                .last("LIMIT " + (max == null ? 3000 : max));

        List<Danmaku> list = this.list(wrapper);
        List<List<Object>> result = new ArrayList<>(list.size());
        for (Danmaku d : list) {
            // DPlayer 期望的数据格式：[time, type, color, author, text]
            List<Object> row = new ArrayList<>(5);
            row.add(d.getTime());
            row.add(d.getType());
            row.add(d.getColor());
            row.add(d.getAuthorId());
            row.add(d.getText());
            result.add(row);
        }
        return result;
    }

}


