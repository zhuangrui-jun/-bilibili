package com.zr.bili.service;

import com.zr.bili.dto.VideoPageDTO;
import com.zr.bili.entity.Video;
import com.zr.bili.result.PageResult;

import java.util.List;

public interface VideoService {

    void saveVideo(Video video);

    List<Video> getAllVideos();

    List<Video> getByCreatorId(Long creatorId);


    Video getVideoDetail(Long id);

    PageResult getPageResult(VideoPageDTO videoPageDTO);
}
