package com.example.backend.photo.entity;

import com.example.backend.trip.entity.Trip;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "trip_photos",
        indexes = {
                @Index(
                        name = "idx_trip_photos_trip_stage",
                        columnList = "trip_id, stage"
                )
        }
)
public class TripPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "photo_type", nullable = false, length = 20)
    private PhotoType photoType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PhotoStage stage;

    @Column(name = "photo_url", nullable = false, columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public TripPhoto() {
    }

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
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

    public PhotoType getPhotoType() {
        return photoType;
    }

    public void setPhotoType(PhotoType photoType) {
        this.photoType = photoType;
    }

    public PhotoStage getStage() {
        return stage;
    }

    public void setStage(PhotoStage stage) {
        this.stage = stage;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
