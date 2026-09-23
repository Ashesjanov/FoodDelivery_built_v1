package com.example.delivery.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 将认证参数解析器注册到 Spring MVC 调用链，使 controller 可直接声明 @CurrentUser。
 * 在 WebMvcConfigurer 初始化阶段调用一次，不影响普通 Spring 参数解析。
 */
@Configuration
public class CurrentUserWebConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public CurrentUserWebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
