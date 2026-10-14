package com.example.backend.vehicle.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class VehicleResponse {

    private UUID id;
    private String make;
    private String model;
    private BigDecimal remainingRangeKm;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;

    public VehicleResponse(
            UUID id, String make,
            String model,
            BigDecimal remainingRangeKm,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.remainingRangeKm = remainingRangeKm;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
    }

    public UUID getId() {
        return id;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getRemainingRangeKm() {
        return remainingRangeKm;
    }

    public BigDecimal getCurrentLatitude() {
        return currentLatitude;
    }

    public BigDecimal getCurrentLongitude() {
        return currentLongitude;
    }
}
