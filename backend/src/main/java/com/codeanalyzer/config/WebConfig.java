package com.codeanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 配置CORS和其他Web相关设置
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置CORS过滤器，允许前端跨域访问API
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源 - 开发环境和生产环境
        config.addAllowedOriginPattern("*");

        // 允许的HTTP方法
        config.addAllowedMethod("*");

        // 允许的头信息
        config.addAllowedHeader("*");

        // 允许发送凭证
        config.setAllowCredentials(true);

        // 预检请求的有效期，单位为秒
        config.setMaxAge(3600L);

        // 将配置应用到所有API路径
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}