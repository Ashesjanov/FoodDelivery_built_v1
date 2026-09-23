package com.example.delivery.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

/** Minimal authenticated identity shared by JWT parsing and request handlers. */
public record UserPrincipal(Long userId, String username, String role) implements Principal {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getName() {
        return username;
    }
}
