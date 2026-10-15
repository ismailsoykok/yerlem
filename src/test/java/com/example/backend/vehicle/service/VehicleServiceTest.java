package com.example.backend.vehicle.service;

import com.example.backend.vehicle.dto.VehicleResponse;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleService Unit Testleri")
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle vehicle1;
    private Vehicle vehicle2;

    @BeforeEach
    void setUp() {
        vehicle1 = new Vehicle();
        vehicle1.setMake("Renault");
        vehicle1.setModel("Zoe");
        vehicle1.setCurrentCity("Ankara");
        vehicle1.setRemainingRangeKm(new BigDecimal("280.00"));
        vehicle1.setCurrentLatitude(new BigDecimal("39.9334"));
        vehicle1.setCurrentLongitude(new BigDecimal("32.8597"));
        vehicle1.setStatus(VehicleStatus.AVAILABLE);
        vehicle1.setTotalRangeKm(new BigDecimal("300.00"));

        vehicle2 = new Vehicle();
        vehicle2.setMake("Togg");
        vehicle2.setModel("T10X");
        vehicle2.setCurrentCity("Istanbul");
        vehicle2.setRemainingRangeKm(new BigDecimal("350.00"));
        vehicle2.setCurrentLatitude(new BigDecimal("41.0082"));
        vehicle2.setCurrentLongitude(new BigDecimal("28.9784"));
        vehicle2.setStatus(VehicleStatus.IN_USE);
        vehicle2.setTotalRangeKm(new BigDecimal("400.00"));
    }

    // -------------------------------------------------------------------------
    // getVehicles - status null iken tüm araçları getirmeli
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status null verilince findAll çağrılmalı ve liste dönmeli")
    void getVehicles_statusNull_returnsAll() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle1, vehicle2));

        List<VehicleResponse> result = vehicleService.getVehicles(null);

        assertThat(result).hasSize(2);
        verify(vehicleRepository, times(1)).findAll();
        verify(vehicleRepository, never()).findAllByStatus(any());
    }

    @Test
    @DisplayName("status null ve liste boş olunca boş liste dönmeli")
    void getVehicles_statusNull_emptyList() {
        when(vehicleRepository.findAll()).thenReturn(Collections.emptyList());

        List<VehicleResponse> result = vehicleService.getVehicles(null);

        assertThat(result).isEmpty();
        verify(vehicleRepository, times(1)).findAll();
    }

    // -------------------------------------------------------------------------
    // getVehicles - status AVAILABLE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status AVAILABLE verilince findAllByStatus(AVAILABLE) çağrılmalı")
    void getVehicles_statusAvailable_callsFindAllByStatus() {
        when(vehicleRepository.findAllByStatus(VehicleStatus.AVAILABLE)).thenReturn(List.of(vehicle1));

        List<VehicleResponse> result = vehicleService.getVehicles(VehicleStatus.AVAILABLE);

        assertThat(result).hasSize(1);
        verify(vehicleRepository, times(1)).findAllByStatus(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).findAll();
    }

    // -------------------------------------------------------------------------
    // getVehicles - status IN_USE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status IN_USE verilince findAllByStatus(IN_USE) çağrılmalı")
    void getVehicles_statusInUse_callsFindAllByStatus() {
        when(vehicleRepository.findAllByStatus(VehicleStatus.IN_USE)).thenReturn(List.of(vehicle2));

        List<VehicleResponse> result = vehicleService.getVehicles(VehicleStatus.IN_USE);

        assertThat(result).hasSize(1);
        verify(vehicleRepository, times(1)).findAllByStatus(VehicleStatus.IN_USE);
    }

    // -------------------------------------------------------------------------
    // getVehicles - status MAINTENANCE, sonuç boş
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status MAINTENANCE verilince findAllByStatus(MAINTENANCE) çağrılmalı ve boş dönmeli")
    void getVehicles_statusMaintenance_emptyList() {
        when(vehicleRepository.findAllByStatus(VehicleStatus.MAINTENANCE)).thenReturn(Collections.emptyList());

        List<VehicleResponse> result = vehicleService.getVehicles(VehicleStatus.MAINTENANCE);

        assertThat(result).isEmpty();
        verify(vehicleRepository, times(1)).findAllByStatus(VehicleStatus.MAINTENANCE);
    }

    // -------------------------------------------------------------------------
    // DTO mapping doğruluğu
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DTO mapping - make, model, city, range, lat, lon doğru maplenmeli")
    void getVehicles_dtoMapping_correct() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle1));

        List<VehicleResponse> result = vehicleService.getVehicles(null);

        assertThat(result).hasSize(1);
        VehicleResponse response = result.get(0);
        assertThat(response.getMake()).isEqualTo("Renault");
        assertThat(response.getModel()).isEqualTo("Zoe");
        assertThat(response.getCurrentCity()).isEqualTo("Ankara");
        assertThat(response.getRemainingRangeKm()).isEqualByComparingTo(new BigDecimal("280.00"));
        assertThat(response.getCurrentLatitude()).isEqualByComparingTo(new BigDecimal("39.9334"));
        assertThat(response.getCurrentLongitude()).isEqualByComparingTo(new BigDecimal("32.8597"));
    }

    @Test
    @DisplayName("DTO mapping - birden fazla araç doğru sırayla maplenmeli")
    void getVehicles_multipleVehicles_allMappedCorrectly() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle1, vehicle2));

        List<VehicleResponse> result = vehicleService.getVehicles(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMake()).isEqualTo("Renault");
        assertThat(result.get(1).getMake()).isEqualTo("Togg");
    }

    @Test
    @DisplayName("DTO mapping - koordinatlar null olan araç da maplenmeli")
    void getVehicles_vehicleWithNullCoordinates_mappedCorrectly() {
        Vehicle vehicleNoCoords = new Vehicle();
        vehicleNoCoords.setMake("Fiat");
        vehicleNoCoords.setModel("500e");
        vehicleNoCoords.setCurrentCity("Izmir");
        vehicleNoCoords.setRemainingRangeKm(new BigDecimal("200.00"));
        vehicleNoCoords.setCurrentLatitude(null);
        vehicleNoCoords.setCurrentLongitude(null);

        when(vehicleRepository.findAll()).thenReturn(List.of(vehicleNoCoords));

        List<VehicleResponse> result = vehicleService.getVehicles(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentLatitude()).isNull();
        assertThat(result.get(0).getCurrentLongitude()).isNull();
    }
}
