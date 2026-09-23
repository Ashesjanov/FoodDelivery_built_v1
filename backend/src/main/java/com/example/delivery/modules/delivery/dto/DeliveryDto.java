package com.example.delivery.modules.delivery.dto;

import com.example.delivery.domain.enums.DeliveryStatus;
import com.example.delivery.domain.enums.RiderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送模块的请求和响应模型：覆盖骑手资料、在线状态、位置、配送备注和履约记录视图。
 * 仅负责字段校验和数据传输，不包含接单状态机，也不参与事务或权限判断。
 */
public final class DeliveryDto {
    private DeliveryDto() {
    }

    public record RiderProfileRequest(@NotBlank @Size(max = 50) String name,
                                      @NotBlank @Size(max = 30) String phone,
                                      @NotBlank @Size(max = 30) String vehicleType) {
    }

    public record RiderStatusRequest(@NotNull RiderStatus status) {
    }

    public record LocationRequest(@NotNull BigDecimal longitude, @NotNull BigDecimal latitude) {
    }

    public record DeliveryNoteRequest(@Size(max = 200) String deliveryNote) {
    }

    public record RiderView(Long id, Long userId, String name, String phone, String vehicleType, RiderStatus status,
                            BigDecimal currentLongitude, BigDecimal currentLatitude, BigDecimal rating,
                            Integer completedCount, Integer activeOrderCount) {
    }

    public record View(Long id, Long orderId, Long riderId, DeliveryStatus status, String pickupCode, String deliveryNote,
                       BigDecimal distanceKm, LocalDateTime acceptedAt, LocalDateTime pickedUpAt, LocalDateTime deliveredAt,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
