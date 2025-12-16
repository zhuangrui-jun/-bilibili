package com.zr.bili.service;

/**
 * 视频转码服务
 */
public interface VideoTranscodeService {

    /**
     * 提交视频转码任务
     * @param videoId 视频ID
     * @param videoUrl 原始视频URL
     * @return 转码任务ID
     */
    String submitTranscodeJob(Long videoId, String videoUrl);

    /**
     * 查询转码任务状态
     * @param jobId 转码任务ID
     * @return 转码结果（包含720p和480p视频URL）
     */
    TranscodeResult queryTranscodeJob(String jobId);
    
    class TranscodeResult {
        private String videoUrl720p;
        private String videoUrl480p;
        private String status; // Processing, Success, Failed
        
        public String getVideoUrl720p() {
            return videoUrl720p;
        }
        
        public void setVideoUrl720p(String videoUrl720p) {
            this.videoUrl720p = videoUrl720p;
        }
        
        public String getVideoUrl480p() {
            return videoUrl480p;
        }
        
        public void setVideoUrl480p(String videoUrl480p) {
            this.videoUrl480p = videoUrl480p;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
}



