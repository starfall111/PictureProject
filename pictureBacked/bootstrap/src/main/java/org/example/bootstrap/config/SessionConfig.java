package org.example.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * @author Zou
 */
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 2592000)
@Configuration
public class SessionConfig {

    // Cookie 配置
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // 自定义 Cookie 名
        serializer.setCookieName("MYAPP_SESSION_ID");
        serializer.setCookiePath("/");
        // 防 XSS
        serializer.setUseHttpOnlyCookie(true);
        // 防 CSRF
//        serializer.setSameSite("Lax");
        return serializer;
    }
}