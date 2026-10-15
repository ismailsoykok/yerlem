package com.example.backend.photo.dto;

import com.example.backend.photo.entity.PhotoStage;
import com.example.backend.photo.entity.PhotoType;

import java.time.LocalDateTime;
import java.util.UUID;

public class TripPhotoResponse {

    private UUID id;
    private UUID tripId;
    private PhotoType photoType;
    private PhotoStage stage;
    private String photoUrl;
    private LocalDateTime uploadedAt;

    public TripPhotoResponse() {
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

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
