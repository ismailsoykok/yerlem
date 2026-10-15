package com.example.backend.wallet.repository;

import com.example.backend.wallet.entity.WalletMovement;
import com.example.backend.wallet.entity.WalletMovementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletMovementRepository
        extends JpaRepository<WalletMovement, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<WalletMovement> findByIdempotencyKey(String idempotencyKey);

    boolean existsByTripIdAndMovementType(
            UUID tripId,
            WalletMovementType movementType
    );
}