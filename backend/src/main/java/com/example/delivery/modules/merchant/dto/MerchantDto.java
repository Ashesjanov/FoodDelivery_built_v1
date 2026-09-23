package com.example.delivery.modules.merchant.dto;

import com.example.delivery.domain.entity.Merchant;
import com.example.delivery.domain.enums.MerchantBusinessStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家和商家分类的请求、响应 DTO 集合，供公开查询和所有者、管理员管理使用。
 * 资料、坐标和费用字段由 Bean Validation 在 REST 边界校验，
 * 响应记录展示营业状态和已持久化的费用。
 */
public final class MerchantDto {
    private MerchantDto() {
    }

    public record SaveRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String description,
            @Size(max = 255) String logoUrl,
            @Size(max = 50) String contactName,
            @NotBlank @Size(max = 30) String contactPhone,
            @Size(max = 50) String province,
            @Size(max = 50) String city,
            @Size(max = 50) String district,
            @NotBlank @Size(max = 200) String address,
            BigDecimal longitude,
            BigDecimal latitude,
            @Size(max = 100) String businessHours,
            @DecimalMin("0.00") BigDecimal minOrderAmount,
            @DecimalMin("0.00") BigDecimal deliveryFee,
            @DecimalMin("0.00") BigDecimal packagingFee,
            List<Long> categoryIds
    ) {
    }

    public record BusinessStatusRequest(@NotNull MerchantBusinessStatus status) {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 50) String name,
            @Size(max = 255) String iconUrl,
            Integer sortOrder,
            Boolean enabled
    ) {
    }

    public record CategoryView(Long id, String name, String iconUrl, Integer sortOrder, Boolean enabled) {
    }

    public record MerchantView(
            Long id, Long ownerId, String name, String description, String logoUrl,
            String contactName, String contactPhone, String province, String city, String district, String address,
            BigDecimal longitude, BigDecimal latitude, MerchantBusinessStatus businessStatus, String businessHours,
            BigDecimal minOrderAmount, BigDecimal deliveryFee, BigDecimal packagingFee, BigDecimal rating,
            Integer monthlySales, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        public static MerchantView from(Merchant merchant) {
            return new MerchantView(merchant.getId(), merchant.getOwnerId(), merchant.getName(), merchant.getDescription(),
                    merchant.getLogoUrl(), merchant.getContactName(), merchant.getContactPhone(), merchant.getProvince(),
                    merchant.getCity(), merchant.getDistrict(), merchant.getAddress(), merchant.getLongitude(), merchant.getLatitude(),
                    merchant.getBusinessStatus(), merchant.getBusinessHours(), merchant.getMinOrderAmount(), merchant.getDeliveryFee(),
                    merchant.getPackagingFee(), merchant.getRating(), merchant.getMonthlySales(), merchant.getCreatedAt(), merchant.getUpdatedAt());
        }
    }

    public record MerchantDetailView(MerchantView merchant, List<Long> categoryIds) {
    }
}
