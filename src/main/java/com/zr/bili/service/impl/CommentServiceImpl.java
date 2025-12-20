package com.zr.bili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zr.bili.context.BaseContext;
import com.zr.bili.dto.CommentPageDTO;
import com.zr.bili.dto.CommentRequest;
import com.zr.bili.entity.Comment;
import com.zr.bili.entity.User;
import com.zr.bili.entity.vo.CommentUserVO;
import com.zr.bili.entity.vo.CommentVO;
import com.zr.bili.mapper.CommentMapper;
import com.zr.bili.result.PageResult;
import com.zr.bili.service.CommentService;
import com.zr.bili.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserService userService;

    @Override
    public PageResult pageQuery(CommentPageDTO commentPageDTO) {
        // ========== 步骤1：分页查询一级评论 ==========
        // 关键：PageHelper.startPage() 必须在查询之前调用
        PageHelper.startPage(commentPageDTO.getPage(), commentPageDTO.getPageSize());
        
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("ref_id", commentPageDTO.getVideoId())
               .eq("parent_id", 0)  // 只查询一级评论（parentId = 0）
               .orderByDesc("created_time");  // 按时间倒序（最新的在前）
        
        Page<Comment> page = (Page<Comment>) commentMapper.selectList(wrapper);
        List<Comment> firstLevelComments = page.getResult();
        
        // 如果没有一级评论，直接返回空结果
        if (firstLevelComments.isEmpty()) {
            return new PageResult(page.getTotal(), new ArrayList<>());
        }
        
        // ========== 步骤2：递归查询所有子评论（扁平化） ==========
        // 获取一级评论的ID列表
        List<Long> firstLevelIds = firstLevelComments.stream()
            .map(Comment::getId)
            .collect(Collectors.toList());
        
        // 递归查询所有子评论（不管多少级）
        List<Comment> allChildComments = getAllChildCommentsRecursive(
            commentPageDTO.getVideoId(), 
            firstLevelIds
        );
        
        // ========== 步骤3：收集所有需要查询的用户ID ==========
        Set<Long> userIds = new HashSet<>();
        
        // 收集一级评论的用户ID
        firstLevelComments.forEach(c -> userIds.add(c.getCreatorId()));
        
        // 收集子评论的用户ID
        allChildComments.forEach(c -> userIds.add(c.getCreatorId()));
        
        // 收集被回复评论的用户ID（用于显示"回复 @xxx"）
        // 注意：这里需要查询被回复的评论来获取其用户ID
        Map<Long, Comment> replyToCommentMap = new HashMap<>();
        for (Comment childComment : allChildComments) {
            if (childComment.getParentId() != null && childComment.getParentId() != 0) {
                // 查询父评论（被回复的评论）
                Comment parentComment = commentMapper.selectById(childComment.getParentId());
                if (parentComment != null) {
                    replyToCommentMap.put(childComment.getParentId(), parentComment);
                    userIds.add(parentComment.getCreatorId());
                }
            }
        }
        
        // ========== 步骤4：批量查询用户信息（避免N+1问题） ==========
        Map<Long, User> userMap = userService.getUsersByIds(new ArrayList<>(userIds));
        
        // ========== 步骤5：构建Map结构，便于快速查找 ==========
        Map<Long, CommentVO> commentMap = new HashMap<>();
        
        // 转换一级评论为CommentVO
        for (Comment comment : firstLevelComments) {
            CommentVO vo = convertToVO(comment, userMap, null);
            commentMap.put(vo.getId(), vo);
        }
        
        // 转换所有子评论为CommentVO（扁平化处理）
        for (Comment comment : allChildComments) {
            // 获取被回复的评论
            Comment replyToComment = replyToCommentMap.get(comment.getParentId());
            CommentVO vo = convertToVO(comment, userMap, replyToComment);
            commentMap.put(vo.getId(), vo);
        }
        
        // ========== 步骤6：构建扁平化树形结构 ==========
        // B站方式：所有子评论（不管多少级）都挂到对应的一级评论下
        for (Comment childComment : allChildComments) {
            CommentVO childVO = commentMap.get(childComment.getId());
            
            // 找到这个子评论对应的根评论（一级评论）
            Long rootCommentId = findRootCommentId(childComment, firstLevelComments);
            CommentVO rootComment = commentMap.get(rootCommentId);
            
            if (rootComment != null) {
                // 初始化children列表
                if (rootComment.getChildren() == null) {
                    rootComment.setChildren(new ArrayList<>());
                }
                // 将所有子评论都添加到一级评论的children中（扁平化）
                rootComment.getChildren().add(childVO);
            }
        }
        
        // ========== 步骤7：对子评论按时间排序 ==========
        for (CommentVO vo : commentMap.values()) {
            if (vo.getChildren() != null && !vo.getChildren().isEmpty()) {
                // 按创建时间正序排序（早的在前）
                vo.getChildren().sort(Comparator.comparing(CommentVO::getCreatedTime));
            }
        }
        
        // ========== 步骤8：构建返回结果 ==========
        // 将一级评论按顺序转换为CommentVO列表
        List<CommentVO> result = firstLevelComments.stream()
            .map(c -> commentMap.get(c.getId()))
            .collect(Collectors.toList());
        
        return new PageResult(page.getTotal(), result);
    }

    /**
     * 递归查询所有子评论（支持多级回复）
     * 
     * 原理：
     * 1. 根据父评论ID列表查询子评论
     * 2. 如果查询到子评论，继续递归查询这些子评论的子评论
     * 3. 将所有层级的子评论都收集起来
     * 
     * @param refId 关联ID（如视频ID）
     * @param parentIds 父评论ID列表
     * @return 所有子评论（包括多级）
     */
    private List<Comment> getAllChildCommentsRecursive(Long refId, List<Long> parentIds) {
        // 如果父评论ID列表为空，递归结束
        if (parentIds == null || parentIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询当前层级的子评论
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("ref_id", refId)
               .in("parent_id", parentIds)
               .orderByAsc("created_time");  // 按时间正序
        
        List<Comment> childComments = commentMapper.selectList(wrapper);
        
        // 如果没有子评论，递归结束
        if (childComments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些子评论的ID，继续递归查询下一级
        List<Long> childIds = childComments.stream()
            .map(Comment::getId)
            .collect(Collectors.toList());
        
        // 递归查询更深层级的子评论
        List<Comment> deeperComments = getAllChildCommentsRecursive(refId, childIds);
        
        // 合并当前层级的子评论和更深层级的子评论
        List<Comment> allComments = new ArrayList<>(childComments);
        allComments.addAll(deeperComments);
        
        return allComments;
    }

    /**
     * 查找根评论ID（一级评论ID）
     * 
     * B站扁平化原理：
     * 不管回复多少级，都要找到它对应的根评论（一级评论）
     * 然后将所有回复都挂到这个根评论下
     * 
     * @param comment 当前评论
     * @param firstLevelComments 一级评论列表
     * @return 根评论ID
     */
    private Long findRootCommentId(Comment comment, List<Comment> firstLevelComments) {
        // 如果当前评论就是一级评论，直接返回
        if (comment.getParentId() == 0 || comment.getParentId() == null) {
            return comment.getId();
        }
        
        // 检查是否在一级评论列表中
        boolean isFirstLevel = firstLevelComments.stream()
            .anyMatch(c -> c.getId().equals(comment.getId()));
        if (isFirstLevel) {
            return comment.getId();
        }
        
        // 递归查找父评论
        Comment parent = commentMapper.selectById(comment.getParentId());
        if (parent == null) {
            // 如果找不到父评论，返回当前评论ID（兜底）
            return comment.getId();
        }
        
        // 继续向上查找
        return findRootCommentId(parent, firstLevelComments);
    }

    /**
     * 将Comment实体转换为CommentVO
     * 
     * @param comment 评论实体
     * @param userMap 用户信息Map（key=用户ID, value=用户对象）
     * @param replyToComment 被回复的评论（用于显示"回复 @xxx"）
     * @return CommentVO对象
     */
    private CommentVO convertToVO(Comment comment, Map<Long, User> userMap, Comment replyToComment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setRefId(comment.getRefId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setCreatedTime(comment.getCreatedTime());
        
        // 设置被回复的用户信息（用于显示"回复 @用户名"）
        if (replyToComment != null) {
            vo.setReplyToCommentId(replyToComment.getId());
            User replyToUser = userMap.get(replyToComment.getCreatorId());
            if (replyToUser != null) {
                CommentUserVO replyToUserVO = new CommentUserVO();
                replyToUserVO.setId(replyToUser.getId());
                replyToUserVO.setUsername(replyToUser.getUsername());
                replyToUserVO.setAvatar(replyToUser.getAvatar());
                vo.setReplyToUser(replyToUserVO);
            }
        }
        
        // 填充评论者信息
        User user = userMap.get(comment.getCreatorId());
        if (user != null) {
            CommentUserVO userVO = new CommentUserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setAvatar(user.getAvatar());
            vo.setUser(userVO);
        }
        
        return vo;
    }

    @Override
    public List<Comment> getAllByRefId(Long refId) {
        return commentMapper.selectList(new QueryWrapper<Comment>().eq("ref_id", refId));
    }

    @Override
    public void saveComment(CommentRequest request) {
        // 获取当前登录用户ID
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        // 创建评论对象
        Comment comment = new Comment();
        comment.setRefId(request.getRefId());
        comment.setContent(request.getContent());
        comment.setCreatorId(currentUserId);
        comment.setCreatedTime(LocalDateTime.now());
        comment.setUpdatedTime(LocalDateTime.now());
        
        // 处理parentId：如果为null或0，表示这是一级评论
        if (request.getParentId() == null || request.getParentId() == 0) {
            comment.setParentId(0L);  // 一级评论的parentId为0
        } else {
            // 验证父评论是否存在
            Comment parentComment = commentMapper.selectById(request.getParentId());
            if (parentComment == null) {
                throw new RuntimeException("父评论不存在");
            }
            // 确保父评论和当前评论关联的是同一个refId
            if (!parentComment.getRefId().equals(request.getRefId())) {
                throw new RuntimeException("父评论与当前评论不属于同一资源");
            }
            comment.setParentId(request.getParentId());
        }
        
        // 保存评论
        commentMapper.insert(comment);
    }
}
