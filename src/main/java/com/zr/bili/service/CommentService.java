package com.zr.bili.service;

import com.zr.bili.dto.CommentPageDTO;
import com.zr.bili.dto.CommentRequest;
import com.zr.bili.entity.Comment;
import com.zr.bili.result.PageResult;

import java.util.List;

public interface CommentService {
    /**
     * 分页查询评论（B站式扁平化结构，包含用户信息）
     */
    PageResult pageQuery(CommentPageDTO commentPageDTO);

    List<Comment> getAllByRefId(Long refId);
    
    /**
     * 保存评论（发送评论或回复评论）
     * @param request 评论请求对象
     */
    void saveComment(CommentRequest request);
}
