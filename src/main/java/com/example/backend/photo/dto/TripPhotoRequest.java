package com.example.backend.photo.dto;

import com.example.backend.photo.entity.PhotoStage;
import com.example.backend.photo.entity.PhotoType;

public class TripPhotoRequest {

    private PhotoType photoType;
    private PhotoStage stage;
    private String photoUrl;

    public TripPhotoRequest() {
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
}
