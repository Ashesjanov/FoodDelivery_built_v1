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

/**
 * 每个请求执行一次的 JWT 认证过滤器，从 Authorization Bearer 令牌建立安全上下文。
 * 位于 Spring Security 用户名密码过滤器之前；令牌有效后还会核对数据库中的账号状态和用户名。
 */
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
            // 先验签并恢复身份，再查库防止已删除、停用或改名账号继续使用旧令牌。
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
            // 无效令牌本身不产生 401 响应；未命中保护规则时保持匿名，由授权规则决定是否拒绝。
            SecurityContextHolder.clearContext();
            log.debug("Rejected invalid bearer token", ex);
        }

        filterChain.doFilter(request, response);
    }
}
