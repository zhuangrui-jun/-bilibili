package com.zr.bili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zr.bili.entity.VideoDraft;
import com.zr.bili.mapper.VideoDraftMapper;
import com.zr.bili.service.VideoDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoDraftServiceImpl implements VideoDraftService {

    private final VideoDraftMapper videoDraftMapper;

    @Override
    public Long saveDraft(VideoDraft draft) {
        draft.setCreatedTime(LocalDateTime.now());
        draft.setUpdatedTime(LocalDateTime.now());
        videoDraftMapper.insert(draft);
        return draft.getId();
    }

    @Override
    public VideoDraft getDraftById(Long id) {
        return videoDraftMapper.selectById(id);
    }

    @Override
    public VideoDraft getDraftsByCreatorId(Long creatorId) {
        QueryWrapper<VideoDraft> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creator_id", creatorId)
                .orderByDesc("created_time")
                .last("LIMIT 1");
        return videoDraftMapper.selectOne(queryWrapper);
    }

    @Override
    public void updateDraft(VideoDraft draft) {
        draft.setUpdatedTime(LocalDateTime.now());
        videoDraftMapper.updateById(draft);
    }

    @Override
    public void deleteDraft(Long id) {
        videoDraftMapper.deleteById(id);
    }
}



















