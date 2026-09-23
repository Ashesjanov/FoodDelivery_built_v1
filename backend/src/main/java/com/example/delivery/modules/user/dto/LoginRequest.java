package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 32) String username,
        @NotBlank @Size(max = 64) String password) {
}
