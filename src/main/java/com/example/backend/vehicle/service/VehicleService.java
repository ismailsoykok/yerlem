package com.example.backend.vehicle.service;

import com.example.backend.vehicle.dto.VehicleResponse;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleResponse> getVehicles(VehicleStatus status) {
        List<Vehicle> vehicles;

        if (status == null) {
            vehicles = vehicleRepository.findAll();
        } else {
            vehicles = vehicleRepository.findAllByStatus(status);
        }

        return vehicles.stream()
                .map(vehicle -> new VehicleResponse(
                        vehicle.getId(),
                        vehicle.getMake(),
                        vehicle.getModel(),
                        vehicle.getCurrentCity(),
                        vehicle.getRemainingRangeKm(),
                        vehicle.getCurrentLatitude(),
                        vehicle.getCurrentLongitude()
                ))
                .toList();
    }
}
