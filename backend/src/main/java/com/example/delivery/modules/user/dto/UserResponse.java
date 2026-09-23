package com.example.delivery.modules.user.dto;

import com.example.delivery.domain.entity.UserAccount;
import com.example.delivery.domain.enums.UserRole;
import com.example.delivery.domain.enums.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String phone,
        String nickname,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getPhone(), user.getNickname(),
                user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
