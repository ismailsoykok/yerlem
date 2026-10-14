package com.example.backend.vehicle.repository;

import com.example.backend.vehicle.entity.VehicleUsageArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleUsageAreaRepository
        extends JpaRepository<VehicleUsageArea, UUID> {

    boolean existsByVehicleIdAndCityNameIgnoreCase(
            UUID vehicleId,
            String cityName
    );
}
