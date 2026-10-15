package com.example.backend.photo.controller;

import com.example.backend.photo.dto.DamageDetectionResponse;
import com.example.backend.photo.dto.TripPhotoRequest;
import com.example.backend.photo.dto.TripPhotoResponse;
import com.example.backend.photo.service.TripPhotoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}")
public class TripPhotoController {

    private final TripPhotoService tripPhotoService;

    public TripPhotoController(TripPhotoService tripPhotoService) {
        this.tripPhotoService = tripPhotoService;
    }

    @PostMapping("/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public TripPhotoResponse uploadPhoto(
            @PathVariable UUID tripId,
            @RequestBody TripPhotoRequest request) {

        return tripPhotoService.uploadPhoto(tripId, request);
    }

    @GetMapping("/photos")
    public List<TripPhotoResponse> getPhotos(@PathVariable UUID tripId) {
        return tripPhotoService.getPhotos(tripId);
    }

    @PostMapping("/damage-detection")
    public DamageDetectionResponse detectDamage(
            @PathVariable UUID tripId) {

        return tripPhotoService.detectDamage(tripId);
    }
}
