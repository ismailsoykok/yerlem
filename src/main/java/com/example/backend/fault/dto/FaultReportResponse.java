package com.example.backend.fault.dto;

import com.example.backend.fault.entity.FaultStatus;
import com.example.backend.fault.entity.FaultType;

import java.time.LocalDateTime;
import java.util.UUID;

public class FaultReportResponse {

    private UUID id;
    private UUID tripId;
    private UUID vehicleId;
    private FaultType faultType;
    private String description;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private FaultStatus status;

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

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public void setFaultType(FaultType faultType) {
        this.faultType = faultType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public FaultStatus getStatus() {
        return status;
    }

    public void setStatus(FaultStatus status) {
        this.status = status;
    }
}