package com.example.delivery.modules.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddressRequest(
        @NotBlank @Size(max = 32) String contactName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone must be a valid mobile number") String phone,
        @NotBlank @Size(max = 32) String province,
        @NotBlank @Size(max = 32) String city,
        @Size(max = 32) String district,
        @NotBlank @Size(max = 255) String detail,
        @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
        @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 6) BigDecimal latitude,
        Boolean isDefault) {
}
