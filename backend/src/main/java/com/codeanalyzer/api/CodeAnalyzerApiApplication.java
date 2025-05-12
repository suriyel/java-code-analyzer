package com.codeanalyzer.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 代码分析系统REST API服务
 */
@SpringBootApplication
public class CodeAnalyzerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAnalyzerApiApplication.class, args);
    }

    /**
     * 配置Bean
     */
    @Configuration
    @EnableWebMvc
    public static class WebConfig implements WebMvcConfigurer {
        @Bean
        public OpenAPI customOpenAPI() {
            return new OpenAPI()
                    .info(new Info()
                            .title("Java代码分析系统API")
                            .description("提供Java代码解析、索引和检索功能")
                            .version("1.0.0"));
        }
    }
}