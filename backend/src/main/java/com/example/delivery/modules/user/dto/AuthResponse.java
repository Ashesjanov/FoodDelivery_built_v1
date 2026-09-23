package com.example.delivery.modules.user.dto;

/**
 * 认证结果，包含 Bearer 令牌、有效期和注册或登录后返回的安全用户资料。
 */
public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
