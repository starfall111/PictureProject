package org.example.server.config;

import org.example.server.Interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/register",
                        "/user/login",
                        "/picture/query",
//                        "/picture/user/{id}",
                        "/category/list",
                        "/tag/list",
                        "/verification/**",
                        "/main/**",
                        // knife4j / swagger 文档相关
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v2/api-docs/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/recommend/query",
                        "/picture/cache/user/query",
                        "/picture/cache/user/detail/*",
                        "/benchmark/**"
                );
        // 新增模块不需要排除路径，均走登录拦截
    }
}
