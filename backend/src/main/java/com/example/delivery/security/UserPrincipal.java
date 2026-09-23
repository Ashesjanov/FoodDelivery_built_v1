package com.example.delivery.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

/**
 * JWT、Spring Security 和业务代码共用的最小登录身份。
 * authorities 将 UserRole 转成 Spring 的 ROLE_* 权限，供 SecurityConfig 的 hasRole 规则使用。
 */
public record UserPrincipal(Long userId, String username, String role) implements Principal {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getName() {
        return username;
    }
}
