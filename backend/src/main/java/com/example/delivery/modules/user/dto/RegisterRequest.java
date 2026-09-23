package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username may contain letters, digits and underscores")
        String username,
        @NotBlank @Size(min = 8, max = 64, message = "password must contain between 8 and 64 characters")
        String password,
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone must be a valid mobile number")
        String phone,
        @Size(max = 32) String nickname) {
}
