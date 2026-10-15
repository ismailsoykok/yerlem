package com.example.backend.photo.repository;

import com.example.backend.photo.entity.TripPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripPhotoRepository extends JpaRepository<TripPhoto, UUID> {

    List<TripPhoto> findAllByTripIdOrderByUploadedAtAsc(UUID tripId);
}
