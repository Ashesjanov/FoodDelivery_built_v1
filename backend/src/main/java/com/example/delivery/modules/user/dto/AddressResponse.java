package com.example.delivery.modules.user.dto;

import com.example.delivery.domain.entity.UserAddress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        Long userId,
        String contactName,
        String phone,
        String province,
        String city,
        String district,
        String detail,
        BigDecimal longitude,
        BigDecimal latitude,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(address.getId(), address.getUserId(), address.getContactName(), address.getPhone(),
                address.getProvince(), address.getCity(), address.getDistrict(), address.getDetail(),
                address.getLongitude(), address.getLatitude(), address.getIsDefault(),
                address.getCreatedAt(), address.getUpdatedAt());
    }
}
