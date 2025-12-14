package com.zr.bili.controller;

import com.zr.bili.entity.Video;
import com.zr.bili.properties.AliProperties;
import com.zr.bili.result.Result;
import com.zr.bili.service.VideoService;
import com.zr.bili.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {

    private final VideoService videoService;

    private final OssUtil ossUtil;

    private final AliProperties ali;
    @PostMapping("/save")
    public Result<String> saveVideo(Video video){
        //todo 上传到oss
         videoService.saveVideo(video);
         return Result.success("视频上传成功");
    }

    //上传图片，待用
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}",file);

        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名的后缀   dfdfdf.png
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构造新文件名称：防止上传到阿里云的文件，因为名字重复导致覆盖的问题
            String objectName = UUID.randomUUID().toString() + extension;

            //文件的请求路径
            //参数：  byte数组，文件对象转成的数组     传上去的图片在阿里云存储空间里面的名字
            String filePath = ossUtil.upload(file.getBytes(), objectName);

            //todo 保存到数据库

            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }

        return Result.error("文件上传失败");
    }

    /**
     * 获取所有视频列表
     */
    @GetMapping("/list")
    public Result<List<Video>> getAllVideos() {
        List<Video> videos = videoService.getAllVideos();
        return Result.success(videos);
    }

}
