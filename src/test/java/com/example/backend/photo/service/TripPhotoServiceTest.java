package com.example.backend.photo.service;

import com.example.backend.photo.dto.DamageDetectionResponse;
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
import com.example.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripPhotoService Unit Testleri")
class TripPhotoServiceTest {

    @Mock
    private TripPhotoRepository tripPhotoRepository;

    @Mock
    private DamageChargeRepository damageChargeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private CurrentUserService currentUserService;

    private TripPhotoService tripPhotoService;

    private UUID userId;
    private UUID tripId;
    private Trip trip;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        User user = new User();
        setId(user, "id", userId);

        trip = new Trip();
        setId(trip, "id", tripId);
        trip.setUser(user);
        trip.setStatus(TripStatus.IN_PROGRESS);

        tripPhotoService = new TripPhotoService(
                tripPhotoRepository,
                damageChargeRepository,
                tripRepository,
                currentUserService,
                new MockDamageDetectionService()
        );
    }

    private static void setId(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TripPhoto photo(PhotoType type, PhotoStage stage, String url) {
        TripPhoto photo = new TripPhoto();
        photo.setTrip(trip);
        photo.setPhotoType(type);
        photo.setStage(stage);
        photo.setPhotoUrl(url);
        return photo;
    }

    // =========================================================================
    // uploadPhoto
    // =========================================================================

    @Test
    @DisplayName("uploadPhoto - eksik alanlarla BAD_REQUEST fırlatmalı")
    void uploadPhoto_invalidRequest_throws400() {
        assertThatThrownBy(() -> tripPhotoService.uploadPhoto(tripId, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("uploadPhoto - seyahat bulunamazsa 404 fırlatmalı")
    void uploadPhoto_tripNotFound_throws404() {
        TripPhotoRequest request = new TripPhotoRequest();
        request.setPhotoType(PhotoType.FRONT_SEAT);
        request.setStage(PhotoStage.START);
        request.setPhotoUrl("mock://front.jpg");

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripPhotoService.uploadPhoto(tripId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("uploadPhoto - başkasının seyahatine FORBIDDEN fırlatmalı")
    void uploadPhoto_notOwner_throws403() {
        TripPhotoRequest request = new TripPhotoRequest();
        request.setPhotoType(PhotoType.EXTERIOR);
        request.setStage(PhotoStage.END);
        request.setPhotoUrl("mock://exterior.jpg");

        when(currentUserService.getUserId()).thenReturn(UUID.randomUUID());
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripPhotoService.uploadPhoto(tripId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("uploadPhoto - başarılı: fotoğraf kaydedilmeli")
    void uploadPhoto_success_savesPhoto() {
        TripPhotoRequest request = new TripPhotoRequest();
        request.setPhotoType(PhotoType.FRONT_SEAT);
        request.setStage(PhotoStage.START);
        request.setPhotoUrl("mock://front.jpg");

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripPhotoRepository.save(any(TripPhoto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TripPhotoResponse response = tripPhotoService.uploadPhoto(tripId, request);

        ArgumentCaptor<TripPhoto> captor = ArgumentCaptor.forClass(TripPhoto.class);
        verify(tripPhotoRepository).save(captor.capture());
        assertThat(captor.getValue().getPhotoType()).isEqualTo(PhotoType.FRONT_SEAT);
        assertThat(response.getPhotoType()).isEqualTo(PhotoType.FRONT_SEAT);
        assertThat(response.getStage()).isEqualTo(PhotoStage.START);
    }

    // =========================================================================
    // detectDamage
    // =========================================================================

    @Test
    @DisplayName("detectDamage - hasar belirteci varsa ek ücret kalemi oluşturmalı")
    void detectDamage_damageDetected_createsCharge() {
        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        when(tripPhotoRepository.findAllByTripIdOrderByUploadedAtAsc(tripId))
                .thenReturn(List.of(
                        photo(PhotoType.EXTERIOR, PhotoStage.START, "mock://clean.jpg"),
                        photo(PhotoType.EXTERIOR, PhotoStage.END, "mock://scratch.jpg")
                ));

        when(damageChargeRepository.existsByTripIdAndPhotoType(
                tripId, PhotoType.EXTERIOR)).thenReturn(false);

        when(damageChargeRepository.save(any(DamageCharge.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DamageDetectionResponse response = tripPhotoService.detectDamage(tripId);

        assertThat(response.getDamageCharges()).hasSize(1);
        assertThat(response.getTotalExtraAmount())
                .isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(damageChargeRepository).save(any(DamageCharge.class));
    }

    @Test
    @DisplayName("detectDamage - hasar yoksa ücret oluşturmamalı")
    void detectDamage_noDamage_noCharge() {
        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        when(tripPhotoRepository.findAllByTripIdOrderByUploadedAtAsc(tripId))
                .thenReturn(List.of(
                        photo(PhotoType.FRONT_SEAT, PhotoStage.START, "mock://a.jpg"),
                        photo(PhotoType.FRONT_SEAT, PhotoStage.END, "mock://b.jpg")
                ));

        DamageDetectionResponse response = tripPhotoService.detectDamage(tripId);

        assertThat(response.getDamageCharges()).isEmpty();
        assertThat(response.getTotalExtraAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        verify(damageChargeRepository, never()).save(any(DamageCharge.class));
    }

    @Test
    @DisplayName("detectDamage - bitiş fotoğrafı yoksa atlamalı")
    void detectDamage_missingEndPhoto_skips() {
        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        when(tripPhotoRepository.findAllByTripIdOrderByUploadedAtAsc(tripId))
                .thenReturn(List.of(
                        photo(PhotoType.FRONT_SEAT, PhotoStage.START, "mock://a.jpg")
                ));

        DamageDetectionResponse response = tripPhotoService.detectDamage(tripId);

        assertThat(response.getDamageCharges()).isEmpty();
        verify(damageChargeRepository, never()).save(any(DamageCharge.class));
    }
}
