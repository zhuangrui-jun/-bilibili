package com.zr.bili.controller;

import com.zr.bili.context.BaseContext;
import com.zr.bili.entity.Video;
import com.zr.bili.entity.VideoDraft;
import com.zr.bili.entity.vo.DraftRequest;
import com.zr.bili.entity.vo.SubmitRequest;
import com.zr.bili.result.Result;
import com.zr.bili.service.VideoDraftService;
import com.zr.bili.service.VideoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoUploadController {

    private final VideoDraftService videoDraftService;
    private final VideoService videoService;

    /**
     * 保存草稿（自动判断是更新还是创建）
     */
    @PostMapping("/draft/save")
    public Result<VideoDraft> saveDraft(@RequestBody DraftRequest request) {
            // 先查询是否有草稿
            VideoDraft existingDraft = videoDraftService.getDraftsByCreatorId(BaseContext.getCurrentId());
            
            VideoDraft draft;
            if (existingDraft != null) {
                // 更新现有草稿
                existingDraft.setTitle(request.getTitle());
                existingDraft.setVideoUrl(request.getVideoUrl());
                existingDraft.setCoverUrl(request.getCoverUrl());
                videoDraftService.updateDraft(existingDraft);
                draft = existingDraft;
            } else {
                // 如果没有草稿，创建新草稿
                draft = new VideoDraft();
                draft.setTitle(request.getTitle());
                draft.setVideoUrl(request.getVideoUrl());
                draft.setCreatorId(BaseContext.getCurrentId());
                draft.setCoverUrl(request.getCoverUrl());
                Long draftId = videoDraftService.saveDraft(draft);
                // 重新查询以获取完整的草稿对象（包含自动生成的id等字段）
                draft = videoDraftService.getDraftById(draftId);
            }

            // 直接返回VideoDraft对象，@JsonSerialize注解会自动生效，id会被序列化为String
            return Result.success(draft);
    }

    /**
     * 根据创建者ID获取草稿列表
     */
    @GetMapping("/draft/list")
    public Result<VideoDraft> getDraftList() {
            VideoDraft draft = videoDraftService.getDraftsByCreatorId(BaseContext.getCurrentId());
            return Result.success(draft);
    }

    @DeleteMapping("/draft/delete")
    public Result<String> deleteDraft() {
            videoDraftService.deleteDraft(BaseContext.getCurrentId());
            return Result.success("草稿删除成功");
    }

    /**
     * 根据ID获取草稿详情
     */
    @GetMapping("/draft/{draftId}")
    public Result<VideoDraft> getDraftDetail(@PathVariable Long draftId) {
            VideoDraft draft = videoDraftService.getDraftById(draftId);
            if (draft == null) {
                return Result.error("草稿不存在");
            }
            // 检查权限：只能查看自己的草稿
            if (!draft.getCreatorId().equals(BaseContext.getCurrentId())) {
                return Result.error("无权限访问此草稿");
            }
            return Result.success(draft);
    }


    /**
     * 提交视频
     * 直接存储用户上传的视频URL
     */
    @PostMapping("/submit")
    public Result<String> submitVideo(@RequestBody SubmitRequest request) {
            // 如果有草稿ID，先获取草稿信息
            VideoDraft draft = null;
            if (request.getDraftId() != null) {
                draft = videoDraftService.getDraftById(request.getDraftId());
                if (draft == null || !draft.getCreatorId().equals(BaseContext.getCurrentId())) {
                    return Result.error("草稿不存在或无权限");
                }
            }
            // 创建视频记录
            Video video = new Video();
            video.setTitle(request.getTitle());
            video.setCreatorId(BaseContext.getCurrentId());
            video.setVideoUrl1(request.getVideoUrl()); // 存储用户上传的视频URL
            video.setCoverUrl(request.getCoverUrl()); // 存储封面URL
            video.setCreatedTime(LocalDateTime.now());
            video.setUpdatedTime(LocalDateTime.now());
            video.setCreatorName(BaseContext.getCurrentUsername());
            video.setTimes(0L);
            video.setIsHot(0);

            // 保存视频
            videoService.saveVideo(video);

            // 如果有草稿，删除草稿
            if (draft != null) {
                videoDraftService.deleteDraft(request.getDraftId());
            }
            return Result.success("视频提交成功");
    }


}

