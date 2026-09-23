package com.example.delivery.modules.user.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.common.PageResponse;
import com.example.delivery.modules.user.dto.AdminUserUpdateRequest;
import com.example.delivery.modules.user.dto.PasswordChangeRequest;
import com.example.delivery.modules.user.dto.UserProfileUpdateRequest;
import com.example.delivery.modules.user.dto.UserResponse;
import com.example.delivery.modules.user.service.UserService;
import com.example.delivery.security.CurrentUser;
import com.example.delivery.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 REST 控制器，统一入口为 {@code /api/users}。
 * 同时提供个人资料、密码接口和仅管理员可用的账号管理接口；REST 边界完成
 * Bean Validation，{@link UserService} 负责事务持久化、密码和归属校验。
 */
@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@CurrentUser UserPrincipal principal) {
        return ApiResponse.ok(userService.get(principal.userId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMe(@CurrentUser UserPrincipal principal,
                                               @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.ok(userService.updateProfile(principal.userId(), request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@CurrentUser UserPrincipal principal,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(principal.userId(), request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(@CurrentUser UserPrincipal principal) {
        userService.deleteOwnAccount(principal.userId());
        return ApiResponse.ok();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> get(@PathVariable long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> adminUpdate(@PathVariable long id,
                                                  @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.ok(userService.adminUpdate(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminDelete(@CurrentUser UserPrincipal principal, @PathVariable long id) {
        userService.adminDelete(principal.userId(), id);
        return ApiResponse.ok();
    }
}
