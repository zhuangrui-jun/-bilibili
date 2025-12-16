package com.zr.bili.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zr.bili.entity.Video;
import com.zr.bili.mapper.VideoMapper;
import com.zr.bili.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;

    @Override
    public void saveVideo(Video video) {
        videoMapper.insert(video);
    }

    @Override
    public List<Video> getAllVideos() {
        return videoMapper.selectList(null);
    }

    @Override
    public List<Video> getByCreatorId(Long creatorId) {
        return videoMapper.selectList(new QueryWrapper<Video>().eq("creator_id", creatorId));
    }

    @Override
    public Video getVideoDetail(Long id) {
        return videoMapper.selectById(id);
    }
}
