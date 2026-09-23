package com.example.delivery.security;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Supports {@code @CurrentUser UserPrincipal}, {@code Long userId}, and {@code String username}. */
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
