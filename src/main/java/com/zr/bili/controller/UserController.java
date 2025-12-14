package com.zr.bili.controller;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.zr.bili.entity.User;
import com.zr.bili.entity.vo.UserVO;
import com.zr.bili.properties.JwtProperties;
import com.zr.bili.result.Result;
import com.zr.bili.service.EmailService;
import com.zr.bili.service.UserService;
import com.zr.bili.utils.CodeUtil;
import com.zr.bili.utils.JwtUtil;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final EmailService emailService;
    private final UserService userService;
    private final JwtProperties jwtProperties;
    // 邮箱正则（校验格式）
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestParam("email")String email){
        if(StrUtil.isBlank(email) || !ReUtil.isMatch(EMAIL_REGEX, email)){
            return Result.error("请输入正确的邮箱格式");
        }
        String code= CodeUtil.generate6DigitCode();
        boolean isSuccess = emailService.sendCodeEmail(email, code);
        if (!isSuccess){
            return Result.error("发送验证码失败，请重试");
        }
        boolean isCache = emailService.cacheCode(email, code);
        if (!isCache){
            return Result.error("缓存失败，请重试");
        }
        return Result.success("验证码已发送，有效时长5分钟");
    }

    //测试接口，主要业务没用到
    @PostMapping("/verifyCode")
    public Result<String> verifyCode(@RequestParam("email")String email,
                             @RequestParam("code")String code){
        if(StringUtils.isEmpty(email)){
            return Result.error("邮箱不能为空");
        }
        if(StringUtils.isEmpty(code)){
            return Result.error("验证码不能为空");
        }
        boolean isMatch = emailService.verifyCode(email, code);
        if (!isMatch){
            return Result.error("验证码错误");
        }
        return Result.success("验证码验证成功");
    }
    @PostMapping("/register")
    public Result<String> register(@RequestParam("email")String email,
                                   @RequestParam("username")String username,
                                   @RequestParam("password")String password,
                                   @RequestParam("code")String code){
        boolean isSuccess = emailService.verifyCode(email, code);
        if (!isSuccess){
            return Result.error("验证码错误");
        }
        userService.register(username,password,email);
        return Result.success("注册成功,请返回登录") ;
    }
    @PostMapping("/login")
    public Result<UserVO> login(@RequestParam("email")String email,
                                @RequestParam("password")String password){
        if(StrUtil.isBlank(email) || !ReUtil.isMatch(EMAIL_REGEX, email)){
            return Result.error("请输入正确的邮箱格式");
        }
        if(StrUtil.isBlank(password)){
            return Result.error("密码不能为空");
        }
        User user = userService.login(email, password);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        UserVO userVO=new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setToken(token);
        return Result.success(userVO);
    }
}
