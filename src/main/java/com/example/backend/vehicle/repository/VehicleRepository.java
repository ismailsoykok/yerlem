package com.example.backend.vehicle.repository;

import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByStatus(VehicleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :vehicleId")
    Optional<Vehicle> findByIdForUpdate(
            @Param("vehicleId") UUID vehicleId
    );
}