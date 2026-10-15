package com.example.backend.trip.controller;

import com.example.backend.alert.dto.TripAlertResponse;
import com.example.backend.alert.service.AlertService;
import com.example.backend.trip.dto.*;
import com.example.backend.trip.service.TripService;
import org.springframework.web.bind.annotation.*;
import com.example.backend.trip.dto.TripCompleteRequest;
import com.example.backend.trip.dto.TripCompleteResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final AlertService alertService;

    public TripController(TripService tripService, AlertService alertService) {
        this.tripService = tripService;
        this.alertService = alertService;
    }

    @PostMapping("/estimate")
    public TripEstimateResponse estimateTrip(
            @RequestBody TripEstimateRequest request) {
        return tripService.estimateTrip(request);
    }
    @PostMapping
    public TripCreateResponse createTrip(
            @RequestBody TripCreateRequest request) {
        return tripService.createTrip(request);
    }

    @PostMapping("/{tripId}/locations")
    public ResponseEntity<?> saveTripLocation(
            @PathVariable UUID tripId,
            @RequestBody TripLocationRequest request) {

        Object response = tripService.saveTripLocation(tripId, request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{tripId}/alerts")
    public List<TripAlertResponse> getTripAlerts(
            @PathVariable UUID tripId) {
        return alertService.getTripAlerts(tripId);
    }


    @PostMapping("/{tripId}/complete")
    public ResponseEntity<TripCompleteResponse> completeTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripCompleteRequest request
    ) {
        return ResponseEntity.ok(
                tripService.completeTrip(tripId, request)
        );
    }


}