package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone must be a valid mobile number")
        String phone,
        @Size(max = 32) String nickname) {
}
