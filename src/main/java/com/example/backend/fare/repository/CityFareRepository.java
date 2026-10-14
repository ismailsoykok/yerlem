package com.example.backend.fare.repository;

import com.example.backend.fare.entity.CityFare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CityFareRepository extends JpaRepository<CityFare, UUID> {

    Optional<CityFare> findByCityNameAndActiveTrue(String cityName);
}