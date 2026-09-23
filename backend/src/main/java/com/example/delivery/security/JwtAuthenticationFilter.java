package com.example.delivery.security;

import com.example.delivery.domain.entity.UserAccount;
import com.example.delivery.domain.enums.UserStatus;
import com.example.delivery.domain.mapper.UserAccountMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Authenticates requests from a valid bearer token and confirms the account is still active. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserAccountMapper userAccountMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountMapper userAccountMapper) {
        this.jwtService = jwtService;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserPrincipal tokenPrincipal = jwtService.parseToken(authorization.substring(BEARER_PREFIX.length()).trim());
            UserAccount user = userAccountMapper.selectById(tokenPrincipal.userId());
            if (user != null && UserStatus.ACTIVE.equals(user.getStatus()) && user.getUsername().equals(tokenPrincipal.username())) {
                UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getRole().name());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.authorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Rejected invalid bearer token", ex);
        }

        filterChain.doFilter(request, response);
    }
}
