package com.zr.bili.interceptor;

import com.zr.bili.context.BaseContext;
import com.zr.bili.properties.JwtProperties;
import com.zr.bili.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //1、从请求头中获取令牌（前端发送的是"token"）
        String token = request.getHeader("token");

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            // 使用与登录时相同的密钥（adminSecretKey）来验证token
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            log.info("当前用户的id：{}", userId);
            BaseContext.setCurrentId(userId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            log.error("jwt校验失败：{}", ex.getMessage());
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}
