
package com.example.backend.trip.dto;

import com.example.backend.trip.entity.TripStatus;
import com.example.backend.vehicle.entity.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripCompleteResponse {

    private UUID tripId;
    private TripStatus status;

    private String originCity;
    private String destinationCity;

    private BigDecimal estimatedDistanceKm;
    private BigDecimal actualDistanceKm;
    private BigDecimal distanceDifferenceKm;

    private Integer estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    private Integer durationDifferenceMinutes;

    private BigDecimal finalAmount;
    private String currency;

    private VehicleStatus vehicleStatus;
    private BigDecimal vehicleRemainingRangeKm;

    private LocalDateTime completedAt;

    public TripCompleteResponse() {
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public BigDecimal getActualDistanceKm() {
        return actualDistanceKm;
    }

    public void setActualDistanceKm(BigDecimal actualDistanceKm) {
        this.actualDistanceKm = actualDistanceKm;
    }

    public BigDecimal getDistanceDifferenceKm() {
        return distanceDifferenceKm;
    }

    public void setDistanceDifferenceKm(BigDecimal distanceDifferenceKm) {
        this.distanceDifferenceKm = distanceDifferenceKm;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Integer getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(Integer actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public Integer getDurationDifferenceMinutes() {
        return durationDifferenceMinutes;
    }

    public void setDurationDifferenceMinutes(Integer durationDifferenceMinutes) {
        this.durationDifferenceMinutes = durationDifferenceMinutes;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public BigDecimal getVehicleRemainingRangeKm() {
        return vehicleRemainingRangeKm;
    }

    public void setVehicleRemainingRangeKm(BigDecimal vehicleRemainingRangeKm) {
        this.vehicleRemainingRangeKm = vehicleRemainingRangeKm;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
