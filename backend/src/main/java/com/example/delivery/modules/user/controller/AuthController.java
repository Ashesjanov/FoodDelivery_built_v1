package com.example.delivery.modules.user.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.modules.user.dto.AuthResponse;
import com.example.delivery.modules.user.dto.LoginRequest;
import com.example.delivery.modules.user.dto.RegisterRequest;
import com.example.delivery.modules.user.dto.UserResponse;
import com.example.delivery.modules.user.service.AuthService;
import com.example.delivery.security.CurrentUser;
import com.example.delivery.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 REST 控制器，统一入口为 {@code /api/auth}。
 * 提供注册、登录和当前用户查询；请求先经 Bean Validation 校验，
 * 注册和登录允许匿名访问，{@code /me} 从 JWT 当前用户解析身份。
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@CurrentUser UserPrincipal principal) {
        return ApiResponse.ok(authService.currentUser(principal.userId()));
    }
}
