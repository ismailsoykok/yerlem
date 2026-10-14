
package com.example.backend.user.repository;

import com.example.backend.user.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID userId
    );
}
