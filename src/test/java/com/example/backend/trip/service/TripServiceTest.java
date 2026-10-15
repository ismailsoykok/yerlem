package com.example.backend.trip.service;

import com.example.backend.alert.entity.TripAlert;
import com.example.backend.alert.service.AlertService;
import com.example.backend.fare.entity.CityFare;
import com.example.backend.fare.entity.TripFare;
import com.example.backend.fare.entity.TripFareStatus;
import com.example.backend.fare.repository.CityFareRepository;
import com.example.backend.fare.repository.TripFareRepository;
import com.example.backend.security.CurrentUserService;
import com.example.backend.trip.dto.*;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.entity.TripLocation;
import com.example.backend.trip.entity.TripStatus;
import com.example.backend.trip.repository.PostgisDistanceRepository;
import com.example.backend.trip.repository.PostgisTripDistanceRepository;
import com.example.backend.trip.repository.TripLocationRepository;
import com.example.backend.trip.repository.TripRepository;
import com.example.backend.user.entity.User;
import com.example.backend.user.entity.UserStatus;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;
import com.example.backend.vehicle.repository.VehicleUsageAreaRepository;
import com.example.backend.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripService Unit Testleri")
class TripServiceTest {

    @Mock
    private TripLocationRepository tripLocationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleUsageAreaRepository vehicleUsageAreaRepository;

    @Mock
    private CityFareRepository cityFareRepository;

    @Mock
    private PostgisDistanceRepository postgisDistanceRepository;

    @Mock
    private PostgisTripDistanceRepository postgisTripDistanceRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripFareRepository tripFareRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private TripService tripService;

    private UUID userId;
    private UUID vehicleId;
    private UUID tripId;
    private User user;
    private Vehicle vehicle;
    private Trip trip;
    private CityFare cityFare;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        user = new User();
        setEntityId(user, userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setPhoneVerified(true);

        vehicle = new Vehicle();
        setEntityId(vehicle, vehicleId);
        vehicle.setCurrentCity("Istanbul");
        vehicle.setCurrentLatitude(new BigDecimal("41.0082"));
        vehicle.setCurrentLongitude(new BigDecimal("28.9784"));
        vehicle.setRemainingRangeKm(new BigDecimal("300.00"));
        vehicle.setTotalRangeKm(new BigDecimal("15000.00"));
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        trip = new Trip();
        setEntityId(trip, tripId);
        trip.setUser(user);
        trip.setVehicle(vehicle);
        trip.setOriginCity("Istanbul");
        trip.setOriginLatitude(new BigDecimal("41.0082"));
        trip.setOriginLongitude(new BigDecimal("28.9784"));
        trip.setDestinationCity("Kocaeli");
        trip.setDestinationLatitude(new BigDecimal("40.8533"));
        trip.setDestinationLongitude(new BigDecimal("29.8815"));
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setEstimatedDistanceKm(new BigDecimal("100.00"));
        trip.setEstimatedDurationMinutes(120);
        trip.setStartedAt(LocalDateTime.now().minusHours(2));

        cityFare = new CityFare();
        cityFare.setCityName("Istanbul");
        cityFare.setPricePerKm(new BigDecimal("15.00"));
        cityFare.setCurrency("TRY");
        cityFare.setActive(true);
    }

    private void setEntityId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // 1. estimateTrip Testleri
    // =========================================================================

    @Nested
    @DisplayName("estimateTrip Testleri")
    class EstimateTripTests {

        @Test
        @DisplayName("request null ise BAD_REQUEST fırlatmalı")
        void estimateTrip_nullRequest_throws400() {
            assertThatThrownBy(() -> tripService.estimateTrip(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("vehicleId null ise BAD_REQUEST fırlatmalı")
        void estimateTrip_nullVehicleId_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("destinationCity null ise BAD_REQUEST fırlatmalı")
        void estimateTrip_nullDestinationCity_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("destinationCity boş veya whitespace ise BAD_REQUEST fırlatmalı")
        void estimateTrip_blankDestinationCity_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("   ");
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("destinationLatitude null ise BAD_REQUEST fırlatmalı")
        void estimateTrip_nullLatitude_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("destinationLongitude null ise BAD_REQUEST fırlatmalı")
        void estimateTrip_nullLongitude_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("40.8533"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("latitude -90'dan küçük ise BAD_REQUEST fırlatmalı")
        void estimateTrip_latitudeBelowMinus90_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("-90.0001"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("latitude 90'dan büyük ise BAD_REQUEST fırlatmalı")
        void estimateTrip_latitudeAbove90_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("90.0001"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("longitude -180'den küçük ise BAD_REQUEST fırlatmalı")
        void estimateTrip_longitudeBelowMinus180_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("-180.0001"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("longitude 180'den büyük ise BAD_REQUEST fırlatmalı")
        void estimateTrip_longitudeAbove180_throws400() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("180.0001"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Araç bulunamadığında NOT_FOUND fırlatmalı")
        void estimateTrip_vehicleNotFound_throws404() {
            TripEstimateRequest req = validEstimateRequest();
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Araç durumu AVAILABLE değilse CONFLICT fırlatmalı")
        void estimateTrip_vehicleNotAvailable_throws409() {
            TripEstimateRequest req = validEstimateRequest();
            vehicle.setStatus(VehicleStatus.IN_USE);
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Aracın konumu veya menzili geçersizse CONFLICT fırlatmalı")
        void estimateTrip_vehicleLocationOrRangeInvalid_throws409() {
            TripEstimateRequest req = validEstimateRequest();

            // latitude null
            vehicle.setCurrentLatitude(null);
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));

            // longitude null
            vehicle.setCurrentLatitude(new BigDecimal("41.0082"));
            vehicle.setCurrentLongitude(null);
            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));

            // remainingRange null
            vehicle.setCurrentLongitude(new BigDecimal("28.9784"));
            vehicle.setRemainingRangeKm(null);
            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));

            // remainingRange <= 0
            vehicle.setRemainingRangeKm(BigDecimal.ZERO);
            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Hedef şehir kullanım alanı dışında ise BAD_REQUEST fırlatmalı")
        void estimateTrip_notInUsageArea_throws400() {
            TripEstimateRequest req = validEstimateRequest();
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli"))
                    .thenReturn(false);

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Aracın bulunduğu şehir için aktif CityFare bulunamazsa CONFLICT fırlatmalı")
        void estimateTrip_noCityFare_throws409() {
            TripEstimateRequest req = validEstimateRequest();
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli"))
                    .thenReturn(true);
            when(cityFareRepository.findByCityNameAndActiveTrue("Istanbul")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Mesafe aracın kalan menzilini aşıyorsa BAD_REQUEST fırlatmalı")
        void estimateTrip_distanceExceedsRemainingRange_throws400() {
            TripEstimateRequest req = validEstimateRequest();
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli"))
                    .thenReturn(true);
            when(cityFareRepository.findByCityNameAndActiveTrue("Istanbul")).thenReturn(Optional.of(cityFare));
            // Mesafe 350 km, menzil 300 km
            when(postgisDistanceRepository.calculateDistanceKm(any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("350.00"));

            assertThatThrownBy(() -> tripService.estimateTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Başarılı tahmin - doğru mesafe, süre, ücret ve para birimi dönmeli")
        void estimateTrip_success() {
            TripEstimateRequest req = validEstimateRequest();
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli"))
                    .thenReturn(true);
            when(cityFareRepository.findByCityNameAndActiveTrue("Istanbul")).thenReturn(Optional.of(cityFare));
            // 100 km mesafe, menzil 300 km
            when(postgisDistanceRepository.calculateDistanceKm(
                    vehicle.getCurrentLongitude(),
                    vehicle.getCurrentLatitude(),
                    req.getDestinationLongitude(),
                    req.getDestinationLatitude()))
                    .thenReturn(new BigDecimal("100.00"));

            TripEstimateResponse response = tripService.estimateTrip(req);

            assertThat(response).isNotNull();
            assertThat(response.getDistanceKm()).isEqualByComparingTo(new BigDecimal("100.00"));
            // 100 / 50 * 60 = 120 dakika
            assertThat(response.getEstimatedDurationMinutes()).isEqualTo(120);
            assertThat(response.getPricePerKm()).isEqualByComparingTo(new BigDecimal("15.00"));
            // 100 * 15 = 1500.00 TL
            assertThat(response.getEstimatedFee()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(response.getCurrency()).isEqualTo("TRY");
        }

        private TripEstimateRequest validEstimateRequest() {
            TripEstimateRequest req = new TripEstimateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));
            return req;
        }
    }

    // =========================================================================
    // 2. createTrip Testleri
    // =========================================================================

    @Nested
    @DisplayName("createTrip Testleri")
    class CreateTripTests {

        @Test
        @DisplayName("request null ise BAD_REQUEST fırlatmalı")
        void createTrip_nullRequest_throws400() {
            assertThatThrownBy(() -> tripService.createTrip(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("vehicleId null ise BAD_REQUEST fırlatmalı")
        void createTrip_nullVehicleId_throws400() {
            TripCreateRequest req = new TripCreateRequest();
            req.setDestinationCity("Kocaeli");

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Kullanıcı bulunamadığında NOT_FOUND fırlatmalı")
        void createTrip_userNotFound_throws404() {
            TripCreateRequest req = validCreateRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Kullanıcı aktif değilse FORBIDDEN fırlatmalı")
        void createTrip_userNotActive_throws403() {
            TripCreateRequest req = validCreateRequest();
            user.setStatus(UserStatus.PENDING);
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Kullanıcı telefonu doğrulanmamışsa FORBIDDEN fırlatmalı")
        void createTrip_phoneNotVerified_throws403() {
            TripCreateRequest req = validCreateRequest();
            user.setPhoneVerified(false);
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Kilitli araç bulunamadığında NOT_FOUND fırlatmalı")
        void createTrip_vehicleNotFoundForUpdate_throws404() {
            TripCreateRequest req = validCreateRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(vehicleRepository.findByIdForUpdate(vehicleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Araç durumu AVAILABLE değilse CONFLICT fırlatmalı")
        void createTrip_vehicleNotAvailable_throws409() {
            TripCreateRequest req = validCreateRequest();
            vehicle.setStatus(VehicleStatus.IN_USE);
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(vehicleRepository.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Cüzdanda bakiye yetersiz ise hata fırlatmalı")
        void createTrip_insufficientBalance_propagatesException() {
            TripCreateRequest req = validCreateRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(vehicleRepository.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));

            // estimateTrip mock'ları
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli")).thenReturn(true);
            when(cityFareRepository.findByCityNameAndActiveTrue("Istanbul")).thenReturn(Optional.of(cityFare));
            when(postgisDistanceRepository.calculateDistanceKm(any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("100.00"));

            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Yetersiz bakiye"))
                    .when(walletService).checkSufficientBalance(eq(userId), any());

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("CityFare bulunamazsa CONFLICT fırlatmalı")
        void createTrip_cityFareNotFound_throws409() {
            TripCreateRequest req = validCreateRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(vehicleRepository.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));

            // estimateTrip içinde ilk çağrıda var, createTrip ikinci çağrıda yok
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli")).thenReturn(true);
            when(cityFareRepository.findByCityNameAndActiveTrue("Istanbul"))
                    .thenReturn(Optional.of(cityFare)) // estimateTrip için
                    .thenReturn(Optional.empty());     // createTrip içindeki kontrol için

            when(postgisDistanceRepository.calculateDistanceKm(any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("100.00"));

            assertThatThrownBy(() -> tripService.createTrip(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Başarılı seyahat başlatma - Trip IN_PROGRESS, TripFare ESTIMATED, Vehicle IN_USE olmalı")
        void createTrip_success() {
            TripCreateRequest req = validCreateRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(vehicleRepository.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));

            // estimateTrip mock'ları
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(vehicleUsageAreaRepository.existsByVehicleIdAndCityNameIgnoreCase(vehicleId, "Kocaeli")).thenReturn(true);
            when(cityFareRepository.findByCityNameAndActiveTrue("Istanbul")).thenReturn(Optional.of(cityFare));
            when(postgisDistanceRepository.calculateDistanceKm(any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("100.00"));

            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
                Trip saved = invocation.getArgument(0);
                setEntityId(saved, tripId);
                return saved;
            });

            TripCreateResponse response = tripService.createTrip(req);

            assertThat(response).isNotNull();
            assertThat(response.getTripId()).isEqualTo(tripId);
            assertThat(response.getStatus()).isEqualTo(TripStatus.IN_PROGRESS.name());
            assertThat(response.getEstimatedDistanceKm()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getEstimatedDurationMinutes()).isEqualTo(120);
            assertThat(response.getEstimatedFee()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(response.getCurrency()).isEqualTo("TRY");

            // Doğrulamalar: Trip ve TripFare kayıtları, Vehicle durum güncellemesi
            ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
            verify(tripRepository).save(tripCaptor.capture());
            Trip capturedTrip = tripCaptor.getValue();
            assertThat(capturedTrip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
            assertThat(capturedTrip.getUser()).isEqualTo(user);
            assertThat(capturedTrip.getVehicle()).isEqualTo(vehicle);
            assertThat(capturedTrip.getOriginCity()).isEqualTo("Istanbul");
            assertThat(capturedTrip.getDestinationCity()).isEqualTo("Kocaeli");

            ArgumentCaptor<TripFare> fareCaptor = ArgumentCaptor.forClass(TripFare.class);
            verify(tripFareRepository).save(fareCaptor.capture());
            TripFare capturedFare = fareCaptor.getValue();
            assertThat(capturedFare.getStatus()).isEqualTo(TripFareStatus.ESTIMATED);
            assertThat(capturedFare.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));

            ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
            verify(vehicleRepository).save(vehicleCaptor.capture());
            assertThat(vehicleCaptor.getValue().getStatus()).isEqualTo(VehicleStatus.IN_USE);
        }

        private TripCreateRequest validCreateRequest() {
            TripCreateRequest req = new TripCreateRequest();
            req.setVehicleId(vehicleId);
            req.setDestinationCity("Kocaeli");
            req.setDestinationLatitude(new BigDecimal("40.8533"));
            req.setDestinationLongitude(new BigDecimal("29.8815"));
            return req;
        }
    }

    // =========================================================================
    // 3. saveTripLocation Testleri
    // =========================================================================

    @Nested
    @DisplayName("saveTripLocation Testleri")
    class SaveTripLocationTests {

        @Test
        @DisplayName("request null ise BAD_REQUEST fırlatmalı")
        void saveTripLocation_nullRequest_throws400() {
            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("latitude null ise BAD_REQUEST fırlatmalı")
        void saveTripLocation_nullLatitude_throws400() {
            TripLocationRequest req = new TripLocationRequest();
            req.setLongitude(new BigDecimal("29.0000"));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("longitude null ise BAD_REQUEST fırlatmalı")
        void saveTripLocation_nullLongitude_throws400() {
            TripLocationRequest req = new TripLocationRequest();
            req.setLatitude(new BigDecimal("41.0000"));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("latitude -90 ile 90 dışındaysa BAD_REQUEST fırlatmalı")
        void saveTripLocation_latitudeOutOfRange_throws400() {
            TripLocationRequest req = new TripLocationRequest();
            req.setLatitude(new BigDecimal("91.0000"));
            req.setLongitude(new BigDecimal("29.0000"));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("longitude -180 ile 180 dışındaysa BAD_REQUEST fırlatmalı")
        void saveTripLocation_longitudeOutOfRange_throws400() {
            TripLocationRequest req = new TripLocationRequest();
            req.setLatitude(new BigDecimal("41.0000"));
            req.setLongitude(new BigDecimal("-181.0000"));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("speedKmh negatif ise BAD_REQUEST fırlatmalı")
        void saveTripLocation_negativeSpeed_throws400() {
            TripLocationRequest req = new TripLocationRequest();
            req.setLatitude(new BigDecimal("41.0000"));
            req.setLongitude(new BigDecimal("29.0000"));
            req.setSpeedKmh(new BigDecimal("-1.00"));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Seyahat bulunamadığında NOT_FOUND fırlatmalı")
        void saveTripLocation_tripNotFound_throws404() {
            TripLocationRequest req = validLocationRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Seyahat oturum açmış kullanıcıya ait değilse FORBIDDEN fırlatmalı")
        void saveTripLocation_differentUser_throws403() {
            TripLocationRequest req = validLocationRequest();
            UUID otherUser = UUID.randomUUID();
            when(currentUserService.getUserId()).thenReturn(otherUser);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Seyahat durumu IN_PROGRESS değilse CONFLICT fırlatmalı")
        void saveTripLocation_tripNotInProgress_throws409() {
            TripLocationRequest req = validLocationRequest();
            trip.setStatus(TripStatus.COMPLETED);
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

            assertThatThrownBy(() -> tripService.saveTripLocation(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Normal konum kaydı - Hız sınırı aşılmadı ve menzil yeterli ise başarı mesajı dönmeli")
        void saveTripLocation_success_normalLocation() {
            TripLocationRequest req = validLocationRequest();
            req.setSpeedKmh(new BigDecimal("70.00"));
            req.setCityName("Istanbul"); // originCity ile aynı -> limit 80

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("20.00"));

            Object result = tripService.saveTripLocation(tripId, req);

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) result;
            assertThat(map.get("message")).isEqualTo("Konum başarıyla kaydedildi.");

            verify(tripLocationRepository).saveAndFlush(any(TripLocation.class));
            verifyNoInteractions(alertService);
        }

        @Test
        @DisplayName("Şehir içi hız sınırı (80 km/s) aşıldığında TripAlert.SPEED_LIMIT_EXCEEDED üretilmeli")
        void saveTripLocation_inCitySpeedLimitExceeded_createsAlert() {
            TripLocationRequest req = validLocationRequest();
            req.setCityName("Istanbul"); // trip.originCity = Istanbul (şehir içi)
            req.setSpeedKmh(new BigDecimal("95.00")); // > 80

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("20.00"));

            tripService.saveTripLocation(tripId, req);

            verify(alertService).createAlert(
                    eq(tripId),
                    eq(TripAlert.SPEED_LIMIT_EXCEEDED),
                    eq("Şehir içi hız sınırı olan 80 km/sa aşıldı."),
                    eq(new BigDecimal("95.00")),
                    eq(new BigDecimal(80))
            );
        }

        @Test
        @DisplayName("Şehir dışı hız sınırı (120 km/s) aşıldığında TripAlert.SPEED_LIMIT_EXCEEDED üretilmeli")
        void saveTripLocation_outOfCitySpeedLimitExceeded_createsAlert() {
            TripLocationRequest req = validLocationRequest();
            req.setCityName("Kocaeli"); // originCity Istanbul -> şehir dışı
            req.setSpeedKmh(new BigDecimal("135.00")); // > 120

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("20.00"));

            tripService.saveTripLocation(tripId, req);

            verify(alertService).createAlert(
                    eq(tripId),
                    eq(TripAlert.SPEED_LIMIT_EXCEEDED),
                    eq("Şehir dışı hız sınırı olan 120 km/sa aşıldı."),
                    eq(new BigDecimal("135.00")),
                    eq(new BigDecimal(120))
            );
        }

        @Test
        @DisplayName("Hız tam sınırda olduğunda (şehir içi 80, şehir dışı 120) uyarı üretilmemeli")
        void saveTripLocation_speedAtLimit_noAlert() {
            TripLocationRequest req = validLocationRequest();
            req.setCityName("Istanbul");
            req.setSpeedKmh(new BigDecimal("80.00"));

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("20.00"));

            tripService.saveTripLocation(tripId, req);

            verifyNoInteractions(alertService);
        }

        @Test
        @DisplayName("Kalan menzil 1 km altına düşerse otomatik completeTrip çağrılmalı")
        void saveTripLocation_rangeUnder1Km_triggersCompleteTrip() {
            TripLocationRequest req = validLocationRequest();
            req.setSpeedKmh(new BigDecimal("50.00"));
            req.setCityName("Istanbul");

            // Aracın menzili 300, yapılan gerçek mesafe 299.5 => kalan 0.5 < 1 km
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId))
                    .thenReturn(new BigDecimal("299.50"));

            // completeTrip içindeki mock'lar
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            TripFare tripFare = new TripFare();
            tripFare.setPricePerKm(new BigDecimal("15.00"));
            tripFare.setStatus(TripFareStatus.ESTIMATED);
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(tripFare));

            Object result = tripService.saveTripLocation(tripId, req);

            // completeTrip çağrıldığı için TripCompleteResponse dönmeli
            assertThat(result).isInstanceOf(TripCompleteResponse.class);
            TripCompleteResponse completeResponse = (TripCompleteResponse) result;
            assertThat(completeResponse.getStatus()).isEqualTo(TripStatus.COMPLETED);

            // walletService.chargeTrip çağrılmış olmalı
            verify(walletService).chargeTrip(eq(userId), eq(trip), any(BigDecimal.class));
        }

        private TripLocationRequest validLocationRequest() {
            TripLocationRequest req = new TripLocationRequest();
            req.setLatitude(new BigDecimal("40.9900"));
            req.setLongitude(new BigDecimal("29.0200"));
            req.setCityName("Istanbul");
            req.setSpeedKmh(new BigDecimal("60.00"));
            return req;
        }
    }

    // =========================================================================
    // 4. completeTrip Testleri
    // =========================================================================

    @Nested
    @DisplayName("completeTrip Testleri")
    class CompleteTripTests {

        @Test
        @DisplayName("request null ise BAD_REQUEST fırlatmalı")
        void completeTrip_nullRequest_throws400() {
            assertThatThrownBy(() -> tripService.completeTrip(tripId, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("latitude null ise BAD_REQUEST fırlatmalı")
        void completeTrip_nullLatitude_throws400() {
            TripCompleteRequest req = new TripCompleteRequest();
            req.setLongitude(new BigDecimal("29.8815"));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("longitude null ise BAD_REQUEST fırlatmalı")
        void completeTrip_nullLongitude_throws400() {
            TripCompleteRequest req = new TripCompleteRequest();
            req.setLatitude(new BigDecimal("40.8533"));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("latitude koordinat sınırları dışındaysa BAD_REQUEST fırlatmalı")
        void completeTrip_latitudeOutOfRange_throws400() {
            TripCompleteRequest req = validCompleteRequest();
            req.setLatitude(new BigDecimal("-91.0000"));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("longitude koordinat sınırları dışındaysa BAD_REQUEST fırlatmalı")
        void completeTrip_longitudeOutOfRange_throws400() {
            TripCompleteRequest req = validCompleteRequest();
            req.setLongitude(new BigDecimal("181.0000"));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("speedKmh negatif ise BAD_REQUEST fırlatmalı")
        void completeTrip_negativeSpeed_throws400() {
            TripCompleteRequest req = validCompleteRequest();
            req.setSpeedKmh(new BigDecimal("-5.00"));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Kilitli seyahat bulunamadığında NOT_FOUND fırlatmalı")
        void completeTrip_tripNotFound_throws404() {
            TripCompleteRequest req = validCompleteRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Seyahat oturum açmış kullanıcıya ait değilse FORBIDDEN fırlatmalı")
        void completeTrip_differentUser_throws403() {
            TripCompleteRequest req = validCompleteRequest();
            UUID otherUserId = UUID.randomUUID();
            when(currentUserService.getUserId()).thenReturn(otherUserId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Seyahat durumu IN_PROGRESS değilse CONFLICT fırlatmalı")
        void completeTrip_tripNotInProgress_throws409() {
            TripCompleteRequest req = validCompleteRequest();
            trip.setStatus(TripStatus.COMPLETED);
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Seyahatin startedAt değeri null ise CONFLICT fırlatmalı")
        void completeTrip_startedAtNull_throws409() {
            TripCompleteRequest req = validCompleteRequest();
            trip.setStartedAt(null);
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Seyahatin startedAt değeri gelecekte ise (completedAt < startedAt) CONFLICT fırlatmalı")
        void completeTrip_completedAtBeforeStartedAt_throws409() {
            TripCompleteRequest req = validCompleteRequest();
            trip.setStartedAt(LocalDateTime.now().plusHours(1));
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Gerçek mesafe hesaplanamadıysa (null veya negatif) UNPROCESSABLE_ENTITY fırlatmalı")
        void completeTrip_actualDistanceNullOrNegative_throws422() {
            TripCompleteRequest req = validCompleteRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));

            // null mesafe
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(null);
            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

            // negatif mesafe
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("-1.00"));
            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("TripFare bulunamazsa UNPROCESSABLE_ENTITY fırlatmalı")
        void completeTrip_tripFareNotFound_throws422() {
            TripCompleteRequest req = validCompleteRequest();
            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("80.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("TripFare durumu ESTIMATED değilse CONFLICT fırlatmalı")
        void completeTrip_tripFareNotEstimated_throws409() {
            TripCompleteRequest req = validCompleteRequest();
            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.FINALIZED);

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("80.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("TripFare pricePerKm null veya negatif ise UNPROCESSABLE_ENTITY fırlatmalı")
        void completeTrip_pricePerKmInvalid_throws422() {
            TripCompleteRequest req = validCompleteRequest();
            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.ESTIMATED);
            fare.setPricePerKm(null);

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("80.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            // null pricePerKm
            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

            // negative pricePerKm
            fare.setPricePerKm(new BigDecimal("-5.00"));
            assertThatThrownBy(() -> tripService.completeTrip(tripId, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Süre tahmini sürenin %20 toleransını aştığında TripAlert.DURATION_EXCEEDED üretilmeli")
        void completeTrip_durationExceeds20PercentTolerance_createsAlert() {
            TripCompleteRequest req = validCompleteRequest();
            // Tahmini süre: 50 dakika. Tolerans eşiği: 50 * 1.20 = 60 dakika.
            trip.setEstimatedDurationMinutes(50);
            // startedAt 80 dakika önce -> gerçek süre 80 dakika > 60 dakika!
            trip.setStartedAt(LocalDateTime.now().minusMinutes(80));

            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.ESTIMATED);
            fare.setPricePerKm(new BigDecimal("10.00"));
            fare.setCurrency("TRY");

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("50.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            tripService.completeTrip(tripId, req);

            verify(alertService).createAlert(
                    eq(tripId),
                    eq(TripAlert.DURATION_EXCEEDED),
                    contains("tolerans sınırını aştı"),
                    any(BigDecimal.class),
                    any(BigDecimal.class)
            );
        }

        @Test
        @DisplayName("Son konumda hız sınırı aşımı varsa SPEED_LIMIT_EXCEEDED uyarısı üretilmeli")
        void completeTrip_speedLimitExceeded_createsAlert() {
            TripCompleteRequest req = validCompleteRequest();
            req.setCityName("Istanbul"); // originCity ile aynı -> limit 80
            req.setSpeedKmh(new BigDecimal("100.00")); // > 80

            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.ESTIMATED);
            fare.setPricePerKm(new BigDecimal("10.00"));
            fare.setCurrency("TRY");

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("50.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            tripService.completeTrip(tripId, req);

            verify(alertService).createAlert(
                    eq(tripId),
                    eq(TripAlert.SPEED_LIMIT_EXCEEDED),
                    eq("Şehir içi hız limiti aşıldı."),
                    eq(new BigDecimal("100.00")),
                    eq(new BigDecimal(80))
            );
        }

        @Test
        @DisplayName("Başarılı seyahat tamamlama - Normal araç AVAILABLE durumuna dönmeli, bakiye tahsil edilmeli")
        void completeTrip_success_vehicleBecomesAvailable() {
            TripCompleteRequest req = validCompleteRequest();
            req.setSpeedKmh(new BigDecimal("50.00"));
            req.setCityName("Kocaeli");

            trip.setStartedAt(LocalDateTime.now().minusMinutes(60));
            trip.setEstimatedDurationMinutes(60);
            vehicle.setStatus(VehicleStatus.IN_USE);
            vehicle.setRemainingRangeKm(new BigDecimal("200.00"));
            vehicle.setTotalRangeKm(new BigDecimal("10000.00"));

            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.ESTIMATED);
            fare.setPricePerKm(new BigDecimal("10.00"));
            fare.setCurrency("TRY");

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("75.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            TripCompleteResponse response = tripService.completeTrip(tripId, req);

            assertThat(response).isNotNull();
            assertThat(response.getTripId()).isEqualTo(tripId);
            assertThat(response.getStatus()).isEqualTo(TripStatus.COMPLETED);
            assertThat(response.getActualDistanceKm()).isEqualByComparingTo(new BigDecimal("75.00"));
            // 75 km * 10 TL = 750 TL
            assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(response.getCurrency()).isEqualTo("TRY");
            assertThat(response.getVehicleStatus()).isEqualTo(VehicleStatus.AVAILABLE);

            // Cüzdandan tahsilat doğrulaması
            verify(walletService).chargeTrip(userId, trip, new BigDecimal("750.00"));

            // Araç güncellemeleri
            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
            // 200 - 75 = 125
            assertThat(vehicle.getRemainingRangeKm()).isEqualByComparingTo(new BigDecimal("125.00"));
            // 10000 + 75 = 10075
            assertThat(vehicle.getTotalRangeKm()).isEqualByComparingTo(new BigDecimal("10075.00"));
            assertThat(vehicle.getCurrentCity()).isEqualTo("Kocaeli");

            // TripFare durumu
            assertThat(fare.getStatus()).isEqualTo(TripFareStatus.FINALIZED);
            assertThat(fare.getTotalAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(fare.getDistanceKm()).isEqualByComparingTo(new BigDecimal("75.00"));

            // Kaydetme doğrulamaları
            verify(tripFareRepository).save(fare);
            verify(tripRepository).save(trip);
            verify(vehicleRepository).save(vehicle);
        }

        @Test
        @DisplayName("Başarılı seyahat tamamlama - Araç arıza nedeniyle MAINTENANCE durumundaysa seyahat bitince MAINTENANCE kalmalı")
        void completeTrip_success_maintenanceVehicleRemainsMaintenance() {
            TripCompleteRequest req = validCompleteRequest();
            req.setSpeedKmh(new BigDecimal("50.00"));

            vehicle.setStatus(VehicleStatus.MAINTENANCE); // Arıza bildirilmiş araç
            vehicle.setRemainingRangeKm(new BigDecimal("100.00"));
            vehicle.setTotalRangeKm(new BigDecimal("10000.00"));

            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.ESTIMATED);
            fare.setPricePerKm(new BigDecimal("10.00"));
            fare.setCurrency("TRY");

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("40.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            TripCompleteResponse response = tripService.completeTrip(tripId, req);

            assertThat(response.getVehicleStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("Başarılı seyahat tamamlama - Gidilen mesafe menzili aştığında kalan menzil sıfırın altına inmemeli (max ZERO)")
        void completeTrip_success_remainingRangeFlooredAtZero() {
            TripCompleteRequest req = validCompleteRequest();
            req.setSpeedKmh(new BigDecimal("50.00"));

            vehicle.setStatus(VehicleStatus.IN_USE);
            vehicle.setRemainingRangeKm(new BigDecimal("30.00")); // Menzil 30 km

            TripFare fare = new TripFare();
            fare.setStatus(TripFareStatus.ESTIMATED);
            fare.setPricePerKm(new BigDecimal("10.00"));
            fare.setCurrency("TRY");

            when(currentUserService.getUserId()).thenReturn(userId);
            when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
            // Gerçek mesafe 50 km (menzilden fazla)
            when(postgisTripDistanceRepository.calculateActualDistanceKm(tripId)).thenReturn(new BigDecimal("50.00"));
            when(tripFareRepository.findByTripId(tripId)).thenReturn(Optional.of(fare));

            tripService.completeTrip(tripId, req);

            assertThat(vehicle.getRemainingRangeKm()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        private TripCompleteRequest validCompleteRequest() {
            TripCompleteRequest req = new TripCompleteRequest();
            req.setLatitude(new BigDecimal("40.8533"));
            req.setLongitude(new BigDecimal("29.8815"));
            req.setCityName("Kocaeli");
            req.setSpeedKmh(new BigDecimal("60.00"));
            return req;
        }
    }
}
