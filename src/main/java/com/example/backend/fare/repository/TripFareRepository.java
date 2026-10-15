package com.example.backend.fare.repository;

import com.example.backend.fare.entity.TripFare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripFareRepository extends JpaRepository<TripFare, UUID> {

    Optional<TripFare> findByTripId(UUID tripId);
}
