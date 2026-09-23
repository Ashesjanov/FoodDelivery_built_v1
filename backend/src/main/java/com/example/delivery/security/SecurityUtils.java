package com.example.delivery.security;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static access to the identity established by {@link JwtAuthenticationFilter}. */
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
