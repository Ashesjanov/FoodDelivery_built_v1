package com.example.delivery.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要当前登录身份的 controller 方法参数，由 {@link CurrentUserArgumentResolver} 注入。
 * 可声明 UserPrincipal、Long 用户 ID 或 String 用户名；未认证请求会得到统一 401 错误。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
