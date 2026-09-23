package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank @Size(max = 64) String currentPassword,
        @NotBlank @Size(min = 8, max = 64) String newPassword) {
}
