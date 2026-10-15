
package com.example.backend.trip.dto;

import com.example.backend.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TripCompleteResponse(
        UUID tripId,
        TripStatus status,
        BigDecimal actualDistanceKm,
        Integer actualDurationMinutes,
        BigDecimal finalAmount,
        String currency,
        LocalDateTime completedAt
) {
}
