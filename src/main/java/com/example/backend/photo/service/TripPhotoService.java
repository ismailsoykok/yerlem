package com.example.backend.photo.service;

import com.example.backend.photo.dto.DamageChargeResponse;
import com.example.backend.photo.dto.DamageDetectionResponse;
import com.example.backend.photo.dto.DamageDetectionResult;
import com.example.backend.photo.dto.TripPhotoRequest;
import com.example.backend.photo.dto.TripPhotoResponse;
import com.example.backend.photo.entity.DamageCharge;
import com.example.backend.photo.entity.PhotoStage;
import com.example.backend.photo.entity.PhotoType;
import com.example.backend.photo.entity.TripPhoto;
import com.example.backend.photo.repository.DamageChargeRepository;
import com.example.backend.photo.repository.TripPhotoRepository;
import com.example.backend.security.CurrentUserService;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.entity.TripStatus;
import com.example.backend.trip.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TripPhotoService {

    private static final Map<PhotoType, BigDecimal> DAMAGE_FEES = Map.of(
            PhotoType.FRONT_SEAT, new BigDecimal("500.00"),
            PhotoType.BACK_SEAT, new BigDecimal("300.00"),
            PhotoType.EXTERIOR, new BigDecimal("1000.00")
    );

    private final TripPhotoRepository tripPhotoRepository;
    private final DamageChargeRepository damageChargeRepository;
    private final TripRepository tripRepository;
    private final CurrentUserService currentUserService;
    private final MockDamageDetectionService damageDetectionService;

    public TripPhotoService(
            TripPhotoRepository tripPhotoRepository,
            DamageChargeRepository damageChargeRepository,
            TripRepository tripRepository,
            CurrentUserService currentUserService,
            MockDamageDetectionService damageDetectionService) {
        this.tripPhotoRepository = tripPhotoRepository;
        this.damageChargeRepository = damageChargeRepository;
        this.tripRepository = tripRepository;
        this.currentUserService = currentUserService;
        this.damageDetectionService = damageDetectionService;
    }

    @Transactional
    public TripPhotoResponse uploadPhoto(
            UUID tripId,
            TripPhotoRequest request) {

        if (request == null
                || request.getPhotoType() == null
                || request.getStage() == null
                || request.getPhotoUrl() == null
                || request.getPhotoUrl().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fotoğraf türü, aşaması ve yolu zorunludur."
            );
        }

        Trip trip = findOwnedTrip(tripId);

        if (trip.getStatus() != TripStatus.IN_PROGRESS
                && trip.getStatus() != TripStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sadece devam eden veya tamamlanmış seyahatlere fotoğraf eklenebilir."
            );
        }

        TripPhoto photo = new TripPhoto();
        photo.setTrip(trip);
        photo.setPhotoType(request.getPhotoType());
        photo.setStage(request.getStage());
        photo.setPhotoUrl(request.getPhotoUrl().trim());

        return toResponse(tripPhotoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public List<TripPhotoResponse> getPhotos(UUID tripId) {

        findOwnedTrip(tripId);

        return tripPhotoRepository.findAllByTripIdOrderByUploadedAtAsc(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DamageDetectionResponse detectDamage(UUID tripId) {

        Trip trip = findOwnedTrip(tripId);

        List<DamageChargeResponse> charges = new ArrayList<>();
        BigDecimal totalExtraAmount = BigDecimal.ZERO;

        // başlangıç ve bitiş fotoğraflarını bölge bazında topluyoruz,
        // aynı bölge için en son yüklenen fotoğraf geçerli sayılır
        Map<PhotoType, String> startUrls = new EnumMap<>(PhotoType.class);
        Map<PhotoType, String> endUrls = new EnumMap<>(PhotoType.class);

        for (TripPhoto photo : tripPhotoRepository
                .findAllByTripIdOrderByUploadedAtAsc(tripId)) {

            if (photo.getStage() == PhotoStage.START) {
                startUrls.put(photo.getPhotoType(), photo.getPhotoUrl());
            } else if (photo.getStage() == PhotoStage.END) {
                endUrls.put(photo.getPhotoType(), photo.getPhotoUrl());
            }
        }

        for (PhotoType photoType : PhotoType.values()) {

            String startUrl = startUrls.get(photoType);
            String endUrl = endUrls.get(photoType);

            if (startUrl == null || endUrl == null) {
                continue;
            }

            DamageDetectionResult result = damageDetectionService.detect(
                    photoType,
                    startUrl,
                    endUrl
            );

            if (!result.isDamaged()) {
                continue;
            }

            // aynı bölge için daha önce ücret eklenmişse tekrar eklemiyoruz
            if (damageChargeRepository.existsByTripIdAndPhotoType(
                    tripId,
                    photoType)) {
                continue;
            }

            DamageCharge charge = new DamageCharge();
            charge.setTrip(trip);
            charge.setPhotoType(photoType);
            charge.setDescription(result.getDescription());
            charge.setAmount(DAMAGE_FEES.get(photoType));
            charge.setCurrency("TRY");

            DamageCharge saved = damageChargeRepository.save(charge);

            charges.add(toResponse(saved));
            totalExtraAmount = totalExtraAmount.add(saved.getAmount());
        }

        DamageDetectionResponse response = new DamageDetectionResponse();
        response.setTripId(tripId);
        response.setDamageCharges(charges);
        response.setTotalExtraAmount(
                totalExtraAmount.setScale(2, RoundingMode.HALF_UP)
        );
        response.setCurrency("TRY");

        return response;
    }

    private Trip findOwnedTrip(UUID tripId) {

        UUID userId = currentUserService.getUserId();

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahat için işlem yapma yetkiniz bulunmuyor."
            );
        }

        return trip;
    }

    private TripPhotoResponse toResponse(TripPhoto photo) {

        TripPhotoResponse response = new TripPhotoResponse();

        response.setId(photo.getId());
        response.setTripId(photo.getTrip().getId());
        response.setPhotoType(photo.getPhotoType());
        response.setStage(photo.getStage());
        response.setPhotoUrl(photo.getPhotoUrl());
        response.setUploadedAt(photo.getUploadedAt());

        return response;
    }

    private DamageChargeResponse toResponse(DamageCharge charge) {

        DamageChargeResponse response = new DamageChargeResponse();

        response.setId(charge.getId());
        response.setPhotoType(charge.getPhotoType());
        response.setDescription(charge.getDescription());
        response.setAmount(charge.getAmount());
        response.setCurrency(charge.getCurrency());
        response.setCreatedAt(charge.getCreatedAt());

        return response;
    }
}
