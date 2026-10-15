package com.example.backend.photo.repository;

import com.example.backend.photo.entity.DamageCharge;
import com.example.backend.photo.entity.PhotoType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DamageChargeRepository
        extends JpaRepository<DamageCharge, UUID> {

    List<DamageCharge> findAllByTripIdOrderByCreatedAtAsc(UUID tripId);

    boolean existsByTripIdAndPhotoType(UUID tripId, PhotoType photoType);
}
