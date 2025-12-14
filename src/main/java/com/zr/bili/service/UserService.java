package com.zr.bili.service;

import com.zr.bili.entity.User;

public interface UserService {

    void register(String username,String password,String email);

    User login(String email, String password);
}
