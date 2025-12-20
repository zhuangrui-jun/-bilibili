package com.zr.bili.service;

import com.zr.bili.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    void register(String username,String password,String email);

    User login(String email, String password);

    User getUserById(Long userId);
    
    /**
     * 批量查询用户信息（避免N+1查询问题）
     * @param userIds 用户ID列表
     * @return Map<用户ID, 用户对象>
     */
    Map<Long, User> getUsersByIds(List<Long> userIds);
}
