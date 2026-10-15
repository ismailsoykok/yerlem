package com.example.backend.alert.dto;

import com.example.backend.alert.entity.TripAlert;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripAlertResponse {

    private UUID id;
    private UUID tripId;
    private TripAlert alertType;
    private String message;
    private BigDecimal recordedValue;
    private BigDecimal thresholdValue;
    private LocalDateTime createdAt;

    public TripAlertResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public TripAlert getAlertType() {
        return alertType;
    }

    public void setAlertType(TripAlert alertType) {
        this.alertType = alertType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getRecordedValue() {
        return recordedValue;
    }

    public void setRecordedValue(BigDecimal recordedValue) {
        this.recordedValue = recordedValue;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}