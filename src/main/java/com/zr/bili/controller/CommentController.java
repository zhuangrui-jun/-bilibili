package com.zr.bili.controller;


import com.zr.bili.dto.CommentPageDTO;
import com.zr.bili.dto.CommentRequest;
import com.zr.bili.entity.Comment;
import com.zr.bili.result.PageResult;
import com.zr.bili.result.Result;
import com.zr.bili.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 根据文章id分页查询
     * @param commentPageDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> getPage(CommentPageDTO commentPageDTO){
        return Result.success(commentService.pageQuery(commentPageDTO));
    }

    /**
     * 根据文章id查询所有评论
     * @param refId
     * @return
     */
    @GetMapping("/list/{refId}")
    public Result<List<Comment>> getAllByRefId(@PathVariable Long refId){
        return Result.success(commentService.getAllByRefId(refId));
    }

    /**
     * 发送评论（一级评论）
     * @param request 评论请求对象
     * @return
     */
    @PostMapping("/add")
    public Result<String> addComment(@RequestBody CommentRequest request) {
        // 验证参数
        if (request.getRefId() == null) {
            return Result.error("关联ID不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        
        // 确保这是一级评论
        request.setParentId(0L);
        
        commentService.saveComment(request);
        return Result.success("评论发送成功");
    }

    /**
     * 回复评论
     * @param request 评论请求对象（必须包含parentId）
     * @return
     */
    @PostMapping("/reply")
    public Result<String> replyComment(@RequestBody CommentRequest request) {
        // 验证参数
        if (request.getRefId() == null) {
            return Result.error("关联ID不能为空");
        }
        if (request.getParentId() == null || request.getParentId() == 0) {
            return Result.error("父评论ID不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return Result.error("回复内容不能为空");
        }
        
        commentService.saveComment(request);
        return Result.success("回复成功");
    }
}
