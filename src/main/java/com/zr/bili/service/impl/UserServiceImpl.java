package com.zr.bili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zr.bili.entity.User;
import com.zr.bili.exception.UserLoginEmailIsEmpty;
import com.zr.bili.exception.UserLoginPasswordWrong;
import com.zr.bili.exception.UserRegisterIsEmpty;
import com.zr.bili.mapper.UserMapper;
import com.zr.bili.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    @Override
    public void register(String username, String password, String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email",email);
        User user = userMapper.selectOne(queryWrapper);
        if(user!=null){
            throw new UserRegisterIsEmpty("该用户的邮箱已注册");
        }
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        User newUser = new User();

        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        userMapper.insert(newUser);
    }

    @Override
    public User login(String email, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email",email);
        User user = userMapper.selectOne(queryWrapper);
        if(user==null){
            throw new UserLoginEmailIsEmpty("该用户的邮箱未注册");
        }
        //密码比对
        //进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if(!user.getPassword().equals(password)){
            throw new UserLoginPasswordWrong("密码错误");
        }
        return user;
    }
}
