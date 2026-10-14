package com.example.backend.vehicle.controller;

import com.example.backend.vehicle.dto.VehicleResponse;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponse> getVehicles(
            @RequestParam(required = false) VehicleStatus status) {
        return vehicleService.getVehicles(status);
    }
}