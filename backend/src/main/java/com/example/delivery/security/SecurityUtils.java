package com.example.delivery.security;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 提供 service 和 controller 读取当前认证身份的静态入口，避免各处直接操作 SecurityContext。
 * 身份由 {@link JwtAuthenticationFilter} 写入；需要强制登录的方法可使用 requireCurrentUserId。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof UserPrincipal principal ? principal : null;
    }

    public static Long currentUserId() {
        UserPrincipal principal = currentUser();
        return principal == null ? null : principal.userId();
    }

    public static long requireCurrentUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static String currentUsername() {
        UserPrincipal principal = currentUser();
        return principal == null ? null : principal.username();
    }

    public static boolean isAuthenticated() {
        return currentUser() != null;
    }

    public static boolean hasRole(String role) {
        UserPrincipal principal = currentUser();
        return principal != null && principal.role().equals(role);
    }

    public static boolean hasAnyRole(String... roles) {
        UserPrincipal principal = currentUser();
        if (principal == null) {
            return false;
        }
        for (String role : roles) {
            if (principal.role().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
