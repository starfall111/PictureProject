package org.example.server.config;

import org.example.server.Interceptor.LoginInterceptor;
import org.example.server.Interceptor.SoftLoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private SoftLoginInterceptor softLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 强制登录拦截器：覆盖除白名单外的所有路径
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
                        "/picture/cache/view/*",
                        "/picture/cache/download/count/*",
                        "/benchmark/**"
                );
        // 软鉴权拦截器：仅对允许游客访问的公共图片接口生效，尝试从 session 填充 UserContext
        // 解决登录用户访问 /picture/cache/user/detail/* 时点赞/收藏状态丢失的问题
        registry.addInterceptor(softLoginInterceptor)
                .addPathPatterns(
                        "/picture/cache/user/query",
                        "/picture/cache/user/detail/*"
                );
        // 新增模块不需要排除路径，均走登录拦截
    }
}
