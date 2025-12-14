package com.zr.bili.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许的源（前端域名，*表示所有，生产环境建议指定具体域名）
        config.addAllowedOrigin("http://localhost:5173");
        // 允许的请求头（*表示所有）
        config.addAllowedHeader("*");
        // 允许的请求方法（GET/POST/PUT/DELETE等，*表示所有）
        config.addAllowedMethod("*");
        // 允许携带Cookie（前后端分离认证时必须开启）
        config.setAllowCredentials(true);
        // 预检请求的缓存时间（秒），减少OPTIONS请求次数
        config.setMaxAge(3600L);

        // 2. 配置生效路径（/**表示所有接口）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 3. 返回CORS过滤器
        return new CorsFilter(source);
    }
}