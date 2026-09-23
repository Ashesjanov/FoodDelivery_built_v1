package com.example.delivery.security;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 将 {@code @CurrentUser} 标注的 controller 参数解析为当前认证主体、用户 ID 或用户名。
 * 由 {@link CurrentUserWebConfig} 注册，身份来源是 {@link SecurityUtils} 中的安全上下文。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal == null) {
            // 使用业务异常可复用全局处理器的稳定 401 响应格式。
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Class<?> type = parameter.getParameterType();
        if (type.isAssignableFrom(UserPrincipal.class)) {
            return principal;
        }
        if (type == Long.class || type == long.class) {
            return principal.userId();
        }
        if (type == String.class) {
            return principal.username();
        }
        throw new IllegalArgumentException("Unsupported @CurrentUser parameter type: " + type.getName());
    }
}
