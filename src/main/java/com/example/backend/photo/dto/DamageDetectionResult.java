package com.example.backend.photo.dto;

import com.example.backend.photo.entity.PhotoType;

public class DamageDetectionResult {

    private final PhotoType photoType;
    private final boolean damaged;
    private final String description;

    public DamageDetectionResult(
            PhotoType photoType,
            boolean damaged,
            String description) {
        this.photoType = photoType;
        this.damaged = damaged;
        this.description = description;
    }

    public PhotoType getPhotoType() {
        return photoType;
    }

    public boolean isDamaged() {
        return damaged;
    }

    public String getDescription() {
        return description;
    }
}
