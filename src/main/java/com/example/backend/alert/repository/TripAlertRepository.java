package com.example.backend.alert.repository;

import com.example.backend.alert.entity.TripAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripAlertRepository
        extends JpaRepository<TripAlertEntity, UUID> {

    List<TripAlertEntity> findAllByTripIdOrderByCreatedAtDesc(UUID tripId);
}