package com.example.backend.trip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripCreateResponse {

    private UUID tripId;
    private String status;
    private BigDecimal estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal estimatedFee;
    private String currency;
    private LocalDateTime startedAt;

    public TripCreateResponse(
            UUID tripId,
            String status,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes,
            BigDecimal estimatedFee,
            String currency,
            LocalDateTime startedAt) {
        this.tripId = tripId;
        this.status = status;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.estimatedFee = estimatedFee;
        this.currency = currency;
        this.startedAt = startedAt;
    }


    public UUID getTripId() {
        return tripId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public BigDecimal getEstimatedFee() {
        return estimatedFee;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}