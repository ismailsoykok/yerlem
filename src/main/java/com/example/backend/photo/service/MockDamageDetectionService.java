package com.example.backend.photo.service;

import com.example.backend.photo.dto.DamageDetectionResult;
import com.example.backend.photo.entity.PhotoType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gerçek görüntü işleme / yapay zeka yerine geçen basit mock servis.
 * Bitiş fotoğrafının yolu/URL'i hasar belirten bir anahtar kelime
 * içeriyorsa hasar tespit edilmiş sayılır.
 */
@Service
public class MockDamageDetectionService {

    private static final List<String> DAMAGE_MARKERS = List.of(
            "damage", "scratch", "crack", "dent", "broken",
            "hasar", "cizik", "çizik", "kirik", "kırık", "ezik"
    );

    public DamageDetectionResult detect(
            PhotoType photoType,
            String startPhotoUrl,
            String endPhotoUrl) {

        if (startPhotoUrl == null || endPhotoUrl == null) {
            return new DamageDetectionResult(photoType, false, null);
        }

        String normalized = endPhotoUrl.toLowerCase();

        boolean damaged = DAMAGE_MARKERS.stream()
                .anyMatch(normalized::contains);

        if (!damaged) {
            return new DamageDetectionResult(photoType, false, null);
        }

        String description = photoType.name()
                + " bölgesinde hasar tespit edildi (mock tespit).";

        return new DamageDetectionResult(photoType, true, description);
    }
}
