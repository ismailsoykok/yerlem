package com.example.backend.trip.dto;

import java.math.BigDecimal;

public class TripEstimateResponse {

    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal pricePerKm;
    private BigDecimal estimatedFee;
    private String currency;

    public TripEstimateResponse(
            BigDecimal distanceKm,
            Integer estimatedDurationMinutes,
            BigDecimal pricePerKm,
            BigDecimal estimatedFee,
            String currency) {
        this.distanceKm = distanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.pricePerKm = pricePerKm;
        this.estimatedFee = estimatedFee;
        this.currency = currency;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public BigDecimal getPricePerKm() {
        return pricePerKm;
    }

    public BigDecimal getEstimatedFee() {
        return estimatedFee;
    }

    public String getCurrency() {
        return currency;
    }
}