package com.example.backend.alert.service;

import com.example.backend.alert.dto.TripAlertResponse;
import com.example.backend.alert.entity.TripAlert;
import com.example.backend.alert.entity.TripAlertEntity;
import com.example.backend.alert.repository.TripAlertRepository;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService Unit Testleri")
class AlertServiceTest {

    @Mock
    private TripAlertRepository tripAlertRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private AlertService alertService;

    private UUID tripId;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        trip = new Trip();
    }

    // -------------------------------------------------------------------------
    // createAlert
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createAlert - seyahat bulunamazsa 404 fırlatmalı")
    void createAlert_tripNotFound_throws404() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                alertService.createAlert(tripId, TripAlert.SPEED_LIMIT_EXCEEDED, "Hız aşıldı", new BigDecimal("90"), new BigDecimal("80"))
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("createAlert - başarılı uyarı oluşturma ve response dönmeli")
    void createAlert_success_returnsResponse() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        TripAlertEntity savedEntity = new TripAlertEntity();
        savedEntity.setTrip(trip);
        savedEntity.setAlertType(TripAlert.SPEED_LIMIT_EXCEEDED);
        savedEntity.setMessage("Hız sınırı aşıldı");
        savedEntity.setRecordedValue(new BigDecimal("90"));
        savedEntity.setThresholdValue(new BigDecimal("80"));

        when(tripAlertRepository.save(any(TripAlertEntity.class))).thenReturn(savedEntity);

        TripAlertResponse result = alertService.createAlert(
                tripId, TripAlert.SPEED_LIMIT_EXCEEDED, "Hız sınırı aşıldı",
                new BigDecimal("90"), new BigDecimal("80")
        );

        assertThat(result).isNotNull();
        assertThat(result.getAlertType()).isEqualTo(TripAlert.SPEED_LIMIT_EXCEEDED);
        assertThat(result.getMessage()).isEqualTo("Hız sınırı aşıldı");
        assertThat(result.getRecordedValue()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(result.getThresholdValue()).isEqualByComparingTo(new BigDecimal("80"));
    }

    @Test
    @DisplayName("createAlert - kaydedilen entity doğru alanlarla oluşturulmalı")
    void createAlert_correctEntitySaved() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        AlertCaptor captor = new AlertCaptor();

        doAnswer(inv -> {
            TripAlertEntity e = inv.getArgument(0);
            captor.entity = e;
            return e;
        }).when(tripAlertRepository).save(any(TripAlertEntity.class));

        alertService.createAlert(
                tripId,
                TripAlert.DURATION_EXCEEDED,
                "Süre aşıldı",
                new BigDecimal("120"),
                new BigDecimal("100")
        );

        assertThat(captor.entity.getAlertType())
                .isEqualTo(TripAlert.DURATION_EXCEEDED);
        assertThat(captor.entity.getMessage())
                .isEqualTo("Süre aşıldı");
        assertThat(captor.entity.getTrip()).isSameAs(trip);
    }

    // recordedValue ve thresholdValue null olabilir (FAULT_REPORTED gibi)
    @Test
    @DisplayName("createAlert - recordedValue ve thresholdValue null gönderilmeli kabul edilmeli")
    void createAlert_nullValues_success() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        TripAlertEntity savedEntity = new TripAlertEntity();
        savedEntity.setTrip(trip);
        savedEntity.setAlertType(TripAlert.FAULT_REPORTED);
        savedEntity.setMessage("Arıza bildirimi");

        when(tripAlertRepository.save(any(TripAlertEntity.class))).thenReturn(savedEntity);

        TripAlertResponse result = alertService.createAlert(tripId, TripAlert.FAULT_REPORTED, "Arıza bildirimi", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getRecordedValue()).isNull();
        assertThat(result.getThresholdValue()).isNull();
    }

    // -------------------------------------------------------------------------
    // getTripAlerts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTripAlerts - seyahat yoksa 404 fırlatmalı")
    void getTripAlerts_tripNotFound_throws404() {
        when(tripRepository.existsById(tripId)).thenReturn(false);

        assertThatThrownBy(() -> alertService.getTripAlerts(tripId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("getTripAlerts - başarılı: uyarı listesi dönmeli")
    void getTripAlerts_success_returnsList() {
        when(tripRepository.existsById(tripId)).thenReturn(true);

        TripAlertEntity alert1 = new TripAlertEntity();
        alert1.setTrip(trip);
        alert1.setAlertType(TripAlert.SPEED_LIMIT_EXCEEDED);
        alert1.setMessage("Hız aşıldı");

        TripAlertEntity alert2 = new TripAlertEntity();
        alert2.setTrip(trip);
        alert2.setAlertType(TripAlert.DURATION_EXCEEDED);
        alert2.setMessage("Süre aşıldı");

        when(tripAlertRepository.findAllByTripIdOrderByCreatedAtDesc(tripId))
                .thenReturn(List.of(alert1, alert2));

        List<TripAlertResponse> result = alertService.getTripAlerts(tripId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAlertType()).isEqualTo(TripAlert.SPEED_LIMIT_EXCEEDED);
        assertThat(result.get(1).getAlertType()).isEqualTo(TripAlert.DURATION_EXCEEDED);
    }

    @Test
    @DisplayName("getTripAlerts - seyahat var ama uyarı yoksa boş liste dönmeli")
    void getTripAlerts_noAlerts_emptyList() {
        when(tripRepository.existsById(tripId)).thenReturn(true);
        when(tripAlertRepository.findAllByTripIdOrderByCreatedAtDesc(tripId))
                .thenReturn(Collections.emptyList());

        List<TripAlertResponse> result = alertService.getTripAlerts(tripId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTripAlerts - DTO dönüşümü doğru olmalı")
    void getTripAlerts_dtoMapping_correct() {
        UUID alertId = UUID.randomUUID();
        UUID tripIdForAlert = UUID.randomUUID();
        Trip alertTrip = new Trip();

        TripAlertEntity alert = new TripAlertEntity();
        alert.setTrip(alertTrip);
        alert.setAlertType(TripAlert.SPEED_LIMIT_EXCEEDED);
        alert.setMessage("Test mesaj");
        alert.setRecordedValue(new BigDecimal("95.50"));
        alert.setThresholdValue(new BigDecimal("80.00"));

        when(tripRepository.existsById(tripId)).thenReturn(true);
        when(tripAlertRepository.findAllByTripIdOrderByCreatedAtDesc(tripId))
                .thenReturn(List.of(alert));

        List<TripAlertResponse> result = alertService.getTripAlerts(tripId);

        TripAlertResponse response = result.get(0);
        assertThat(response.getMessage()).isEqualTo("Test mesaj");
        assertThat(response.getAlertType()).isEqualTo(TripAlert.SPEED_LIMIT_EXCEEDED);
        assertThat(response.getRecordedValue()).isEqualByComparingTo(new BigDecimal("95.50"));
        assertThat(response.getThresholdValue()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    // -------------------------------------------------------------------------
    // Yardımcı iç sınıf (lambda'da capture için)
    // -------------------------------------------------------------------------
    static class AlertCaptor {
        TripAlertEntity entity;
    }
}
