package com.zr.bili.controller;

import com.zr.bili.context.BaseContext;
import com.zr.bili.dto.LikeStatusDTO;
import com.zr.bili.result.Result;
import com.zr.bili.service.VlikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/video/like")
public class VlikeController {

    private final VlikeService vlikeService;

    /**
     * 点赞/取消点赞
     * @param videoId 视频ID
     * @return 返回点赞状态和点赞数
     */
    @PostMapping("/{videoId}")
    public Result<LikeStatusDTO> toggleLike(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            boolean isLiked = vlikeService.toggleLike(videoId, userId);
            LikeStatusDTO status = vlikeService.getLikeStatus(videoId, userId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("点赞操作失败，videoId: {}, userId: {}", videoId, userId, e);
            return Result.error("操作失败，请重试");
        }
    }

    /**
     * 获取用户对视频的点赞状态和点赞数
     * @param videoId 视频ID
     * @return 点赞状态和点赞数
     */
    @GetMapping("/{videoId}")
    public Result<LikeStatusDTO> getLikeStatus(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            LikeStatusDTO status = vlikeService.getLikeStatus(videoId, userId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取点赞状态失败，videoId: {}, userId: {}", videoId, userId, e);
            return Result.error("获取失败，请重试");
        }
    }

    /**
     * 获取视频的点赞数（不需要登录）
     * @param videoId 视频ID
     * @return 点赞数
     */
    @GetMapping("/count/{videoId}")
    public Result<Integer> getLikeCount(@PathVariable Long videoId) {
        try {
            Integer count = vlikeService.getLikeCount(videoId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取点赞数失败，videoId: {}", videoId, e);
            return Result.error("获取失败，请重试");
        }
    }
}

