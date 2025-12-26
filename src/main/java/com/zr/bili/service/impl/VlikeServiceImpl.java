package com.zr.bili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zr.bili.dto.LikeStatusDTO;
import com.zr.bili.entity.Vlike;
import com.zr.bili.mapper.VlikeMapper;
import com.zr.bili.service.VlikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class VlikeServiceImpl implements VlikeService {

    private final StringRedisTemplate redis;
    private final VlikeMapper vlikeMapper;

    private static final String LIKE_SET = "like:video:";
    private static final String LIKE_COUNT = "like:count:";
    private static final int EXPIRE_DAYS = 7;

    /**
     * 点赞/取消点赞
     * 只更新Redis，数据库更新由定时任务处理
     */
    @Override
    public boolean toggleLike(Long videoId, Long userId) {
        String setKey = LIKE_SET + videoId;
        String countKey = LIKE_COUNT + videoId;
        String userIdStr = userId.toString();

        // 如果Redis过期，轻量级恢复（只恢复点赞总数和当前用户状态）
        if (!redis.hasKey(setKey)) {
            restoreFromDB(videoId, userId, true);
        }

        // 检查是否已点赞
        Boolean isLiked = redis.opsForSet().isMember(setKey, userIdStr);
        if (isLiked == null) {
            isLiked = false; // Redis异常时默认未点赞
        }

        if (isLiked) {
            // 取消点赞：只更新Redis
            redis.opsForSet().remove(setKey, userIdStr);
            Long newCount = redis.opsForValue().decrement(countKey);
            // 防止点赞数为负数
            if (newCount != null && newCount < 0) {
                redis.opsForValue().set(countKey, "0");
            }
            log.info("用户 {} 对视频 {} 取消点赞（已更新Redis，等待定时任务同步到数据库）", userId, videoId);
            return false;
        } else {
            // 点赞：只更新Redis
            redis.opsForSet().add(setKey, userIdStr);
            redis.opsForValue().increment(countKey);
            redis.expire(setKey, EXPIRE_DAYS, TimeUnit.DAYS);
            redis.expire(countKey, EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("用户 {} 对视频 {} 点赞成功（已更新Redis，等待定时任务同步到数据库）", userId, videoId);
            return true;
        }
    }

    /**
     * 获取用户点赞状态和点赞数
     */
    @Override
    public LikeStatusDTO getLikeStatus(Long videoId, Long userId) {
        String setKey = LIKE_SET + videoId;
        String countKey = LIKE_COUNT + videoId;

        // Redis过期时轻量级恢复（只恢复当前用户状态和点赞总数）
        if (!redis.hasKey(setKey)) {
            restoreFromDB(videoId, userId, true);
        }

        LikeStatusDTO dto = new LikeStatusDTO();
        dto.setIsLiked(Boolean.TRUE.equals(redis.opsForSet().isMember(setKey, userId.toString())));
        
        String countStr = redis.opsForValue().get(countKey);
        dto.setLikeCount(countStr != null ? Integer.parseInt(countStr) : 0);
        
        return dto;
    }

    /**
     * 获取视频的点赞数（不需要用户登录）
     */
    @Override
    public Integer getLikeCount(Long videoId) {
        String countKey = LIKE_COUNT + videoId;
        String setKey = LIKE_SET + videoId;

        try {
            // Redis过期时轻量级恢复（只恢复点赞总数，不需要用户状态）
            if (!redis.hasKey(setKey)) {
                restoreFromDB(videoId, null, false);
            }

            String countStr = redis.opsForValue().get(countKey);
            if (countStr != null) {
                return Integer.parseInt(countStr);
            }
        } catch (Exception e) {
            log.error("从Redis获取点赞数失败，videoId: {}", videoId, e);
        }

        // 降级：直接查数据库
        try {
            QueryWrapper<Vlike> wrapper = new QueryWrapper<>();
            wrapper.eq("video_id", videoId).eq("is_liked", true);
            Long count = vlikeMapper.selectCount(wrapper);
            
            // 同步到Redis
            if (count != null) {
                redis.opsForValue().set(countKey, count.toString());
                redis.expire(countKey, EXPIRE_DAYS, TimeUnit.DAYS);
            }
            
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("从数据库获取点赞数失败，videoId: {}", videoId, e);
            return 0;
        }
    }

    /**
     * 从数据库轻量级恢复数据到Redis
     * 只恢复当前用户状态和点赞总数，不加载所有用户ID（性能优化）
     * 
     * @param videoId 视频ID
     * @param userId 用户ID（查询场景需要，获取点赞数时可为null）
     * @param needUserStatus 是否需要恢复用户状态（查询场景为true，只获取点赞数时为false）
     */
    private void restoreFromDB(Long videoId, Long userId, boolean needUserStatus) {
        String setKey = LIKE_SET + videoId;
        String countKey = LIKE_COUNT + videoId;

        try {
            // 1. 查询点赞总数（所有场景都需要，使用COUNT查询，性能好）
            QueryWrapper<Vlike> countWrapper = new QueryWrapper<>();
            countWrapper.eq("video_id", videoId).eq("is_liked", true);
            Long count = vlikeMapper.selectCount(countWrapper);
            
            if (needUserStatus && userId != null) {
                // 2. 查询场景：需要恢复当前用户的点赞状态
                QueryWrapper<Vlike> userWrapper = new QueryWrapper<>();
                userWrapper.eq("video_id", videoId)
                           .eq("user_id", userId)
                           .eq("is_liked", true);
                Vlike userLike = vlikeMapper.selectOne(userWrapper);
                
                // 如果当前用户已点赞，添加到Set；否则创建空Set
                if (userLike != null) {
                    redis.opsForSet().add(setKey, userId.toString());
                    log.info("轻量级恢复视频 {} 的点赞数据（用户：{}已点赞，总数：{}）", videoId, userId, count);
                } else {
                    // 创建空Set（使用占位符）
                    redis.opsForSet().add(setKey, "placeholder");
                    redis.opsForSet().remove(setKey, "placeholder");
                    log.info("轻量级恢复视频 {} 的点赞数据（用户：{}未点赞，总数：{}）", videoId, userId, count);
                }
            } else {
                // 3. 只获取点赞数场景：只需要初始化空Set
                redis.opsForSet().add(setKey, "placeholder");
                redis.opsForSet().remove(setKey, "placeholder");
                log.info("轻量级恢复视频 {} 的点赞数据（只恢复总数：{}）", videoId, count);
            }
            
            // 4. 设置点赞数
            redis.opsForValue().set(countKey, String.valueOf(count != null ? count : 0));
            
            // 5. 设置过期时间
            redis.expire(setKey, EXPIRE_DAYS, TimeUnit.DAYS);
            redis.expire(countKey, EXPIRE_DAYS, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("轻量级恢复点赞数据失败，videoId: {}, userId: {}", videoId, userId, e);
        }
    }

    /**
     * 同步Redis中的点赞数据到数据库
     * 定时任务调用，将Redis中的所有点赞数据同步到数据库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncLikeDataToDatabase() {
        log.info("开始同步点赞数据到数据库");

        try {
            Set<String> likeKeys = scanKeys(LIKE_SET + "*");
            int syncCount = 0;
            int updateCount = 0;
            int insertCount = 0;

            for (String setKey : likeKeys) {
                try {
                    String videoIdStr = setKey.substring(LIKE_SET.length());
                    Long videoId = Long.parseLong(videoIdStr);

                    Set<String> userIds = redis.opsForSet().members(setKey);
                    if (userIds == null || userIds.isEmpty()) {
                        continue;
                    }

                    // 检查是否只有占位符（轻量级恢复后的空Set）
                    boolean onlyPlaceholder = userIds.size() == 1 && userIds.contains("placeholder");
                    
                    if (onlyPlaceholder) {
                        // Set中只有占位符，说明Redis过期后还没有用户操作
                        // 这种情况下，数据库应该是最新的，不需要同步
                        // 但需要验证数据一致性
                        String countKey = LIKE_COUNT + videoId;
                        String countStr = redis.opsForValue().get(countKey);
                        int redisCount = countStr != null ? Integer.parseInt(countStr) : 0;

                        QueryWrapper<Vlike> countQuery = new QueryWrapper<>();
                        countQuery.eq("video_id", videoId);
                        countQuery.eq("is_liked", true);
                        Long dbCount = vlikeMapper.selectCount(countQuery);

                        if (dbCount == null || dbCount != redisCount) {
                            log.warn("视频 {} 的点赞数不一致（只有占位符）：Redis={}, 数据库={}", videoId, redisCount, dbCount);
                        }
                        continue;
                    }

                    // 同步每个用户的点赞状态到数据库（Redis中有 = 已点赞）
                    for (String userIdStr : userIds) {
                        // 跳过占位符（虽然上面已经处理了，但保留作为安全措施）
                        if ("placeholder".equals(userIdStr)) {
                            continue;
                        }
                        Long userId = Long.parseLong(userIdStr);
                        
                        // 同步到数据库（点赞状态为true）
                        boolean inserted = syncUserLikeStatus(videoId, userId, true);
                        if (inserted) {
                            insertCount++;
                        } else {
                            updateCount++;
                        }
                        syncCount++;
                    }

                    // 处理取消点赞的情况：Redis中没有，但数据库中有is_liked=true的记录
                    // 需要将数据库中is_liked=true但Redis中没有的用户，更新为false
                    QueryWrapper<Vlike> dbQuery = new QueryWrapper<>();
                    dbQuery.eq("video_id", videoId).eq("is_liked", true);
                    List<Vlike> dbLikedList = vlikeMapper.selectList(dbQuery);
                    
                    if (dbLikedList != null) {
                        for (Vlike dbLike : dbLikedList) {
                            String dbUserIdStr = dbLike.getUserId().toString();
                            // 如果数据库中有，但Redis中没有，说明用户取消了点赞
                            if (!userIds.contains(dbUserIdStr)) {
                                syncUserLikeStatus(videoId, dbLike.getUserId(), false);
                                updateCount++;
                            }
                        }
                    }

                    // 验证数据一致性
                    String countKey = LIKE_COUNT + videoId;
                    String countStr = redis.opsForValue().get(countKey);
                    int redisCount = countStr != null ? Integer.parseInt(countStr) : userIds.size();

                    QueryWrapper<Vlike> countQuery = new QueryWrapper<>();
                    countQuery.eq("video_id", videoId);
                    countQuery.eq("is_liked", true);
                    Long dbCount = vlikeMapper.selectCount(countQuery);

                    if (dbCount == null || dbCount != redisCount) {
                        log.warn("视频 {} 的点赞数不一致：Redis={}, 数据库={}", videoId, redisCount, dbCount);
                    }

                } catch (Exception e) {
                    log.error("同步视频点赞数据失败，key: {}", setKey, e);
                }
            }

            log.info("同步完成，共处理 {} 条点赞记录（新增：{}，更新：{}）", syncCount, insertCount, updateCount);

        } catch (Exception e) {
            log.error("同步Redis点赞数据到数据库失败", e);
        }
    }

    /**
     * 同步单个用户的点赞状态到数据库
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param isLiked 是否点赞
     * @return true表示新增记录，false表示更新记录
     */
    private boolean syncUserLikeStatus(Long videoId, Long userId, boolean isLiked) {
        QueryWrapper<Vlike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("video_id", videoId);
        queryWrapper.eq("user_id", userId);
        Vlike existingLike = vlikeMapper.selectOne(queryWrapper);

        if (existingLike != null) {
            // 记录存在，检查状态是否需要更新
            if (existingLike.getIsLiked() == null || !existingLike.getIsLiked().equals(isLiked)) {
                existingLike.setIsLiked(isLiked);
                existingLike.setCreatedTime(LocalDateTime.now());
                vlikeMapper.updateById(existingLike);
            }
            return false; // 更新操作
        } else {
            // 记录不存在，创建新记录
            Vlike newLike = new Vlike();
            newLike.setVideoId(videoId);
            newLike.setUserId(userId);
            newLike.setIsLiked(isLiked);
            newLike.setCreatedTime(LocalDateTime.now());
            vlikeMapper.insert(newLike);
            return true; // 新增操作
        }
    }

    /**
     * 扫描Redis中的key（避免使用keys命令，使用scan）
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            Cursor<String> cursor = redis.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)
                            .build()
            );

            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
            cursor.close();
        } catch (Exception e) {
            log.error("扫描Redis key失败", e);
        }
        return keys;
    }
}
