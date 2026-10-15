package com.example.backend.alert.entity;

import com.example.backend.trip.entity.Trip;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "trip_alerts",
        indexes = {
                @Index(
                        name = "idx_trip_alerts_trip_time",
                        columnList = "trip_id, created_at"
                )
        }
)
public class TripAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 40)
    private TripAlert alertType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "recorded_value", precision = 10, scale = 2)
    private BigDecimal recordedValue;

    @Column(name = "threshold_value", precision = 10, scale = 2)
    private BigDecimal thresholdValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TripAlertEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
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
}