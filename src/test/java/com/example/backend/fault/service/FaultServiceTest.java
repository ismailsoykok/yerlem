package com.example.backend.fault.service;

import com.example.backend.alert.entity.TripAlert;
import com.example.backend.alert.service.AlertService;
import com.example.backend.fault.dto.FaultReportRequest;
import com.example.backend.fault.dto.FaultReportResponse;
import com.example.backend.fault.entity.FaultReport;
import com.example.backend.fault.entity.FaultStatus;
import com.example.backend.fault.entity.FaultType;
import com.example.backend.fault.repository.FaultReportRepository;
import com.example.backend.security.CurrentUserService;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.entity.TripStatus;
import com.example.backend.trip.repository.TripRepository;
import com.example.backend.user.entity.User;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FaultService Unit Testleri")
class FaultServiceTest {

    @Mock
    private FaultReportRepository faultReportRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private FaultService faultService;

    private UUID userId;
    private UUID tripId;
    private User user;
    private Vehicle vehicle;
    private Trip trip;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        user = new User();
        // Reflection ile id set ediyoruz çünkü setter yok
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.IN_USE);

        trip = new Trip();
        try {
            var field = Trip.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(trip, tripId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        trip.setUser(user);
        trip.setVehicle(vehicle);
        trip.setStatus(TripStatus.IN_PROGRESS);
    }

    // =========================================================================
    // reportFault
    // =========================================================================

    @Test
    @DisplayName("reportFault - request null ise BAD_REQUEST fırlatmalı")
    void reportFault_nullRequest_throws400() {
        assertThatThrownBy(() -> faultService.reportFault(tripId, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("reportFault - faultType null ise BAD_REQUEST fırlatmalı")
    void reportFault_nullFaultType_throws400() {
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(null);

        assertThatThrownBy(() -> faultService.reportFault(tripId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("reportFault - seyahat bulunamazsa 404 fırlatmalı")
    void reportFault_tripNotFound_throws404() {
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.BATTERY);

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> faultService.reportFault(tripId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("reportFault - seyahat başkasına aitse FORBIDDEN fırlatmalı")
    void reportFault_tripNotBelongsToUser_throws403() {
        UUID anotherUserId = UUID.randomUUID();
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.TIRE);

        when(currentUserService.getUserId()).thenReturn(anotherUserId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> faultService.reportFault(tripId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("reportFault - seyahat IN_PROGRESS değilse CONFLICT fırlatmalı")
    void reportFault_tripNotInProgress_throws409() {
        trip.setStatus(TripStatus.COMPLETED);
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.MOTOR);

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> faultService.reportFault(tripId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("reportFault - başarılı: FaultReport kaydedilmeli, araç MAINTENANCE olmalı, alert üretilmeli")
    void reportFault_success_savesFaultReport_setsMaintenanceAndAlert() {
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.BATTERY);
        request.setDescription("Batarya boşaldı");

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        faultService.reportFault(tripId, request);

        // FaultReport kaydedilmeli
        ArgumentCaptor<FaultReport> faultCaptor = ArgumentCaptor.forClass(FaultReport.class);
        verify(faultReportRepository).save(faultCaptor.capture());
        FaultReport savedReport = faultCaptor.getValue();
        assertThat(savedReport.getFaultType()).isEqualTo(FaultType.BATTERY);
        assertThat(savedReport.getDescription()).isEqualTo("Batarya boşaldı");

        // Araç durumu MAINTENANCE olmalı
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        verify(vehicleRepository).save(vehicle);

        // Alert üretilmeli
        verify(alertService).createAlert(
                eq(tripId),
                eq(TripAlert.FAULT_REPORTED),
                any(String.class),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("reportFault - açıklama boş string ise description set edilmemeli")
    void reportFault_blankDescription_descriptionNotSet() {
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.DOOR);
        request.setDescription("   ");

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        faultService.reportFault(tripId, request);

        ArgumentCaptor<FaultReport> captor = ArgumentCaptor.forClass(FaultReport.class);
        verify(faultReportRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    @DisplayName("reportFault - açıklama null ise description null bırakılmalı")
    void reportFault_nullDescription_descriptionNull() {
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.OTHER);
        request.setDescription(null);

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        faultService.reportFault(tripId, request);

        ArgumentCaptor<FaultReport> captor = ArgumentCaptor.forClass(FaultReport.class);
        verify(faultReportRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    @DisplayName("reportFault - alert mesajı arıza türünü içermeli")
    void reportFault_alertMessageContainsFaultType() {
        FaultReportRequest request = new FaultReportRequest();
        request.setFaultType(FaultType.TIRE);

        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        faultService.reportFault(tripId, request);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertService).createAlert(
                eq(tripId),
                eq(TripAlert.FAULT_REPORTED),
                messageCaptor.capture(),
                isNull(),
                isNull()
        );
        assertThat(messageCaptor.getValue()).contains("TIRE");
    }

    // =========================================================================
    // getTripFaults
    // =========================================================================

    @Test
    @DisplayName("getTripFaults - seyahat bulunamazsa 404 fırlatmalı")
    void getTripFaults_tripNotFound_throws404() {
        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> faultService.getTripFaults(tripId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("getTripFaults - seyahat başkasına aitse FORBIDDEN fırlatmalı")
    void getTripFaults_tripNotBelongsToUser_throws403() {
        UUID anotherUserId = UUID.randomUUID();
        when(currentUserService.getUserId()).thenReturn(anotherUserId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> faultService.getTripFaults(tripId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("getTripFaults - başarılı: arıza listesi dönmeli ve DTO mapping doğru olmalı")
    void getTripFaults_success_returnsMappedList() {
        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        FaultReport fault1 = new FaultReport();
        fault1.setTrip(trip);
        fault1.setVehicle(vehicle);
        fault1.setFaultType(FaultType.BATTERY);
        fault1.setStatus(FaultStatus.OPEN);

        when(faultReportRepository.findAllByTripIdOrderByReportedAtDesc(tripId))
                .thenReturn(List.of(fault1));

        List<FaultReportResponse> result = faultService.getTripFaults(tripId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFaultType()).isEqualTo(FaultType.BATTERY);
        assertThat(result.get(0).getStatus()).isEqualTo(FaultStatus.OPEN);
    }

    @Test
    @DisplayName("getTripFaults - arıza yoksa boş liste dönmeli")
    void getTripFaults_noFaults_emptyList() {
        when(currentUserService.getUserId()).thenReturn(userId);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(faultReportRepository.findAllByTripIdOrderByReportedAtDesc(tripId))
                .thenReturn(Collections.emptyList());

        List<FaultReportResponse> result = faultService.getTripFaults(tripId);

        assertThat(result).isEmpty();
    }
}
