package com.example.backend.fault.repository;

import com.example.backend.fault.entity.FaultReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaultReportRepository extends JpaRepository<FaultReport, UUID> {

    List<FaultReport> findAllByTripIdOrderByReportedAtDesc(UUID tripId);

    boolean existsByTripIdAndStatusNot(
            UUID tripId,
            com.example.backend.fault.entity.FaultStatus status
    );
}