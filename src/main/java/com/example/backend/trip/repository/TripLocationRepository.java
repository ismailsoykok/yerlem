package com.example.backend.trip.repository;

import com.example.backend.trip.entity.TripLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripLocationRepository
        extends JpaRepository<TripLocation, UUID> {

    List<TripLocation> findAllByTripIdOrderByRecordedAtAsc(UUID tripId);
}