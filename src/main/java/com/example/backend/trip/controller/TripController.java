package com.example.backend.trip.controller;

import com.example.backend.trip.dto.TripEstimateRequest;
import com.example.backend.trip.dto.TripEstimateResponse;
import com.example.backend.trip.service.TripService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/estimate")
    public TripEstimateResponse estimateTrip(
            @RequestBody TripEstimateRequest request) {
        return tripService.estimateTrip(request);
    }
}