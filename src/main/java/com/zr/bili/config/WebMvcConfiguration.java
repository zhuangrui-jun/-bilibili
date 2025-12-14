package com.zr.bili.config;

import com.zr.bili.interceptor.JwtTokenInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtTokenInterceptor jwtTokenAdminInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/**");
    }
}
