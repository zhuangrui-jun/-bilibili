package com.zr.bili.service;

import com.zr.bili.dto.LikeStatusDTO;

public interface VlikeService {

    /**
     * 点赞/取消点赞
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return true表示点赞成功，false表示取消点赞成功
     */
    boolean toggleLike(Long videoId, Long userId);

    /**
     * 获取用户点赞状态和点赞数
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 点赞状态DTO
     */
    LikeStatusDTO getLikeStatus(Long videoId, Long userId);

    /**
     * 获取视频的点赞数（不需要用户登录）
     * @param videoId 视频ID
     * @return 点赞数
     */
    Integer getLikeCount(Long videoId);

    /**
     * 同步Redis中的点赞数据到数据库
     * 定时任务调用，用于持久化数据（兜底方案）
     */
    void syncLikeDataToDatabase();
}
