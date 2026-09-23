package com.example.delivery.modules.delivery.dto;

import com.example.delivery.domain.enums.DeliveryStatus;
import com.example.delivery.domain.enums.RiderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
