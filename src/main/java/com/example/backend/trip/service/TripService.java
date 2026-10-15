
package com.example.backend.trip.service;
import com.example.backend.alert.entity.TripAlert;
import com.example.backend.alert.service.AlertService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.example.backend.trip.repository.PostgisTripDistanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.backend.fare.entity.CityFare;
import com.example.backend.fare.entity.TripFare;
import com.example.backend.fare.entity.TripFareStatus;
import com.example.backend.fare.repository.CityFareRepository;
import com.example.backend.fare.repository.TripFareRepository;
import com.example.backend.security.CurrentUserService;
import com.example.backend.trip.dto.*;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.entity.TripStatus;
import com.example.backend.trip.repository.PostgisDistanceRepository;
import com.example.backend.trip.repository.TripRepository;
import com.example.backend.user.entity.User;
import com.example.backend.user.entity.UserStatus;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;
import com.example.backend.vehicle.repository.VehicleUsageAreaRepository;
import com.example.backend.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.backend.trip.dto.TripLocationRequest;
import com.example.backend.trip.entity.TripLocation;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.entity.TripStatus;
import com.example.backend.trip.repository.TripLocationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TripService {

    private final TripLocationRepository tripLocationRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleUsageAreaRepository vehicleUsageAreaRepository;
    private final CityFareRepository cityFareRepository;
    private final PostgisDistanceRepository postgisDistanceRepository;
    private final PostgisTripDistanceRepository  postgisTripDistanceRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final TripRepository tripRepository;
    private final TripFareRepository tripFareRepository;
    private final AlertService alertService;

    public TripService(TripLocationRepository tripLocationRepository,
                       VehicleRepository vehicleRepository,
                       VehicleUsageAreaRepository vehicleUsageAreaRepository,
                       CityFareRepository cityFareRepository,
                       PostgisDistanceRepository postgisDistanceRepository, PostgisTripDistanceRepository postgisTripDistanceRepository,
                       CurrentUserService currentUserService,
                       UserRepository userRepository,
                       WalletService walletService,
                       TripRepository tripRepository,
                       TripFareRepository tripFareRepository, AlertService alertService) {

        this.tripLocationRepository = tripLocationRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleUsageAreaRepository = vehicleUsageAreaRepository;
        this.cityFareRepository = cityFareRepository;
        this.postgisDistanceRepository = postgisDistanceRepository;
        this.postgisTripDistanceRepository = postgisTripDistanceRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.tripRepository = tripRepository;
        this.tripFareRepository = tripFareRepository;
        this.alertService = alertService;
    }

    public TripEstimateResponse estimateTrip(TripEstimateRequest request) {

        if (request == null
                || request.getVehicleId() == null
                || request.getDestinationCity() == null
                || request.getDestinationCity().isBlank()
                || request.getDestinationLatitude() == null
                || request.getDestinationLongitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Araç ve hedef bilgileri zorunludur."
            );
        }

        BigDecimal latitude = request.getDestinationLatitude();
        BigDecimal longitude = request.getDestinationLongitude();

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hedef koordinatları geçersiz."
            );
        }

        UUID vehicleId = request.getVehicleId();

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Araç bulunamadı."
                ));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Araç şu anda kullanıma uygun değil."
            );
        }

        if (vehicle.getCurrentLatitude() == null
                || vehicle.getCurrentLongitude() == null
                || vehicle.getRemainingRangeKm() == null
                || vehicle.getRemainingRangeKm()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aracın konum veya menzil bilgileri geçersiz."
            );
        }

        String destinationCity = request.getDestinationCity().trim();

        boolean allowedArea =
                vehicleUsageAreaRepository
                        .existsByVehicleIdAndCityNameIgnoreCase(
                                vehicleId,
                                destinationCity
                        );

        if (!allowedArea) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hedef şehir aracın kullanım alanı dışında."
            );
        }

        CityFare cityFare = cityFareRepository
                .findByCityNameAndActiveTrue(vehicle.getCurrentCity())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Aracın bulunduğu şehir için aktif ücret tarifesi bulunamadı."
                ));

        BigDecimal distanceKm =
                postgisDistanceRepository.calculateDistanceKm(
                        vehicle.getCurrentLongitude(),
                        vehicle.getCurrentLatitude(),
                        longitude,
                        latitude
                ).setScale(2, RoundingMode.HALF_UP);

        if (distanceKm.compareTo(vehicle.getRemainingRangeKm()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hedef, aracın kalan menzilini aşıyor."
            );
        }

        int durationMinutes = distanceKm
                .divide(BigDecimal.valueOf(50), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.CEILING)
                .intValue();

        BigDecimal estimatedFee = distanceKm
                .multiply(cityFare.getPricePerKm())
                .setScale(2, RoundingMode.HALF_UP);

        return new TripEstimateResponse(
                distanceKm,
                durationMinutes,
                cityFare.getPricePerKm(),
                estimatedFee,
                cityFare.getCurrency()
        );
    }

    @Transactional
    public TripCreateResponse createTrip(TripCreateRequest request) {

        if (request == null || request.getVehicleId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Araç bilgisi zorunludur."
            );
        }

        // 1. Giriş yapmış kullanıcıyı bul.
        UUID userId = currentUserService.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kullanıcı bulunamadı."
                ));

        // 2. Kullanıcının aktif ve telefonunun doğrulanmış olduğunu kontrol et.
        if (user.getStatus() != UserStatus.ACTIVE
                || !user.isPhoneVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seyahat başlatmak için hesabınız aktif ve telefonunuz doğrulanmış olmalıdır."
            );
        }

        // 3. Aracı kilitli şekilde getir.
        Vehicle vehicle = vehicleRepository
                .findByIdForUpdate(request.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Araç bulunamadı."
                ));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Araç başka bir seyahatte veya kullanıma kapalı."
            );
        }

        // 4. Ücreti istemciden kabul etme; sunucuda yeniden hesapla.
        TripEstimateRequest estimateRequest = new TripEstimateRequest();
        estimateRequest.setVehicleId(request.getVehicleId());
        estimateRequest.setDestinationCity(request.getDestinationCity());
        estimateRequest.setDestinationLatitude(
                request.getDestinationLatitude()
        );
        estimateRequest.setDestinationLongitude(
                request.getDestinationLongitude()
        );

        TripEstimateResponse estimate = estimateTrip(estimateRequest);

        // 5. Tahmini ücret kadar bakiye olup olmadığını kontrol et.

        walletService.checkSufficientBalance(
                userId,
                estimate.getEstimatedFee()
        );

        CityFare cityFare = cityFareRepository
                .findByCityNameAndActiveTrue(vehicle.getCurrentCity())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Aktif ücret tarifesi bulunamadı."
                ));

        // 6. Seyahat kaydını oluştur.
        LocalDateTime startedAt = LocalDateTime.now();

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setVehicle(vehicle);
        trip.setOriginCity(vehicle.getCurrentCity());
        trip.setOriginLatitude(vehicle.getCurrentLatitude());
        trip.setOriginLongitude(vehicle.getCurrentLongitude());
        trip.setDestinationCity(request.getDestinationCity().trim());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setEstimatedDistanceKm(estimate.getDistanceKm());
        trip.setEstimatedDurationMinutes(
                estimate.getEstimatedDurationMinutes()
        );
        trip.setStartedAt(startedAt);

        trip = tripRepository.save(trip);

        // 7. Seyahatin ücret hesaplamasını ayrı tabloda sakla.
        TripFare tripFare = new TripFare();
        tripFare.setTrip(trip);
        tripFare.setCityFare(cityFare);
        tripFare.setDistanceKm(estimate.getDistanceKm());
        tripFare.setPricePerKm(estimate.getPricePerKm());
        tripFare.setTotalAmount(estimate.getEstimatedFee());
        tripFare.setCurrency(estimate.getCurrency());
        tripFare.setStatus(TripFareStatus.ESTIMATED);

        tripFareRepository.save(tripFare);

        // 8. Aracı kullanımda olarak işaretle.
        vehicle.setStatus(VehicleStatus.IN_USE);
        vehicleRepository.save(vehicle);

        // 9. İstemciye seyahat bilgisini döndür.
        return new TripCreateResponse(
                trip.getId(),
                trip.getStatus().name(),
                estimate.getDistanceKm(),
                estimate.getEstimatedDurationMinutes(),
                estimate.getEstimatedFee(),
                estimate.getCurrency(),
                startedAt
        );
    }


    @Transactional
    public Object saveTripLocation(
            UUID tripId,
            TripLocationRequest request) {

        // 1. İstek ve koordinatları doğrula.
        if (request == null
                || request.getLatitude() == null
                || request.getLongitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Konum koordinatları zorunludur."
            );
        }

        BigDecimal latitude = request.getLatitude();
        BigDecimal longitude = request.getLongitude();
        BigDecimal speedKmh = request.getSpeedKmh();

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Konum koordinatları geçersiz."
            );
        }

        if (speedKmh != null
                && speedKmh.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hız negatif olamaz."
            );
        }

        // 2. Giriş yapan kullanıcıyı belirle.
        UUID userId = currentUserService.getUserId();

        // 3. Seyahati bul.
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        // 4. Seyahatin kullanıcıya ait olduğunu kontrol et.
        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahate konum gönderme yetkiniz yok."
            );
        }

        // 5. Seyahatin devam ettiğini kontrol et.
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Yalnızca devam eden seyahatlere konum eklenebilir."
            );
        }

        // 6. Konumu kaydet.
        TripLocation location = new TripLocation();
        location.setTrip(trip);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setCityName(
                request.getCityName() == null
                        ? null
                        : request.getCityName().trim()
        );
        location.setSpeedKmh(speedKmh);

        tripLocationRepository.saveAndFlush(location);

        // 7. Hız sınırını kontrol et.
        if (speedKmh != null && request.getCityName() != null) {

            String cityName = request.getCityName().trim();

            // Başlangıç şehriyle aynıysa şehir içi kabul et.
            boolean inCity = cityName.equalsIgnoreCase(
                    trip.getOriginCity()
            );

            BigDecimal speedLimit = inCity
                    ? BigDecimal.valueOf(80)
                    : BigDecimal.valueOf(120);

            if (speedKmh.compareTo(speedLimit) > 0) {

                String message = inCity
                        ? "Şehir içi hız sınırı olan 80 km/sa aşıldı."
                        : "Şehir dışı hız sınırı olan 120 km/sa aşıldı.";

                alertService.createAlert(
                        tripId,
                        TripAlert.SPEED_LIMIT_EXCEEDED,
                        message,
                        speedKmh,
                        speedLimit
                );
            }
        }

        // 8. Kalan menzili kontrol et.
        BigDecimal actualDistanceKm =
                postgisTripDistanceRepository
                        .calculateActualDistanceKm(trip.getId());

        if (actualDistanceKm != null) {

            BigDecimal remainingRangeKm =
                    trip.getVehicle().getRemainingRangeKm();

            BigDecimal estimatedRemainingRange =
                    remainingRangeKm.subtract(actualDistanceKm);

            // Kalan menzil 1 km'nin altına düştüyse yolculuğu bitir.
            if (estimatedRemainingRange.compareTo(BigDecimal.ONE) < 0) {

                TripCompleteRequest completeRequest =
                        new TripCompleteRequest();

                completeRequest.setLatitude(latitude);
                completeRequest.setLongitude(longitude);
                completeRequest.setCityName(request.getCityName());
                completeRequest.setSpeedKmh(speedKmh);

                return completeTrip(tripId, completeRequest);
            }
        }
        return Map.of("message", "Konum başarıyla kaydedildi.");
    }


    @Transactional
    public TripCompleteResponse completeTrip(
            UUID tripId,
            TripCompleteRequest request
    ) {
        // 1. İstek ve koordinat kontrolleri
        if (request == null
                || request.getLatitude() == null
                || request.getLongitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Son konum bilgisi zorunludur."
            );
        }

        if (request.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || request.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0
                || request.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || request.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Geçersiz koordinat bilgisi."
            );
        }

        if (request.getSpeedKmh() != null
                && request.getSpeedKmh().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hız negatif olamaz."
            );
        }

        // 2. JWT üzerinden mevcut kullanıcıyı al
        UUID userId = currentUserService.getUserId();

        // 3. Seyahati kilitleyerek getir.
        // Aynı seyahat için eşzamanlı tamamlama isteklerinin
        // ikinci kez işlem yapmasını engeller.
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        // 4. Seyahatin bu kullanıcıya ait olduğunu doğrula
        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahati tamamlama yetkiniz bulunmuyor."
            );
        }

        // 5. Yalnızca devam eden seyahat tamamlanabilir
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Yalnızca devam eden seyahatler tamamlanabilir."
            );
        }

        LocalDateTime completedAt = LocalDateTime.now();

        if (trip.getStartedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seyahatin başlangıç zamanı bulunamadı."
            );
        }

        if (completedAt.isBefore(trip.getStartedAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seyahat bitiş zamanı başlangıç zamanından önce olamaz."
            );
        }

        // 6. Son konumu seyahat zaman çizelgesine kaydet
        TripLocation finalLocation = new TripLocation();
        finalLocation.setTrip(trip);
        finalLocation.setLatitude(request.getLatitude());
        finalLocation.setLongitude(request.getLongitude());
        finalLocation.setCityName(
                request.getCityName() == null
                        ? null
                        : request.getCityName().trim()
        );
        finalLocation.setSpeedKmh(request.getSpeedKmh());
        finalLocation.setRecordedAt(completedAt);

        tripLocationRepository.saveAndFlush(finalLocation);

        // 7. Hız sınırını kontrol et ve gerekiyorsa uyarı oluştur
        if (request.getSpeedKmh() != null) {
            boolean isSameCity = request.getCityName() != null
                    && trip.getOriginCity() != null
                    && request.getCityName().trim()
                    .equalsIgnoreCase(trip.getOriginCity().trim());

            BigDecimal speedLimit = isSameCity
                    ? BigDecimal.valueOf(80)
                    : BigDecimal.valueOf(120);

            if (request.getSpeedKmh().compareTo(speedLimit) > 0) {
                alertService.createAlert(
                        trip.getId(),
                        TripAlert.SPEED_LIMIT_EXCEEDED,
                        isSameCity
                                ? "Şehir içi hız limiti aşıldı."
                                : "Şehir dışı hız limiti aşıldı.",
                        request.getSpeedKmh(),
                        speedLimit
                );
            }
        }

        // 8. Konum kayıtlarından gerçek mesafeyi hesapla
        BigDecimal actualDistanceKm =
                postgisTripDistanceRepository
                        .calculateActualDistanceKm(trip.getId());

        if (actualDistanceKm == null
                || actualDistanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Seyahatin gerçek mesafesi hesaplanamadı."
            );
        }

        actualDistanceKm = actualDistanceKm.setScale(
                2,
                RoundingMode.HALF_UP
        );

        // 9. Gerçek seyahat süresini hesapla
        long durationMinutes = Duration.between(
                trip.getStartedAt(),
                completedAt
        ).toMinutes();

        int actualDurationMinutes = Math.toIntExact(durationMinutes);

        // 10. Tahmini süreyle gerçekleşen süreyi karşılaştır.
        // Tolerans: tahmini sürenin %20 fazlası.
        Integer estimatedDuration = trip.getEstimatedDurationMinutes();

        if (estimatedDuration != null && estimatedDuration > 0) {
            BigDecimal durationThreshold = BigDecimal
                    .valueOf(estimatedDuration)
                    .multiply(BigDecimal.valueOf(1.20))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal actualDuration = BigDecimal.valueOf(
                    actualDurationMinutes
            );

            if (actualDuration.compareTo(durationThreshold) > 0) {
                alertService.createAlert(
                        trip.getId(),
                        TripAlert.DURATION_EXCEEDED,
                        "Seyahat süresi tahmini sürenin %20 üzerindeki "
                                + "tolerans sınırını aştı.",
                        actualDuration,
                        durationThreshold
                );
            }
        }

        // 11. Seyahat ücretini kontrol et
        TripFare tripFare = tripFareRepository.findByTripId(trip.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Seyahat ücreti bulunamadı."
                ));

        if (tripFare.getStatus() != TripFareStatus.ESTIMATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seyahat ücreti daha önce işlenmiş veya geçersiz durumda."
            );
        }

        if (tripFare.getPricePerKm() == null
                || tripFare.getPricePerKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Kilometre tarifesi geçersiz olduğu için ücret hesaplanamadı."
            );
        }

        // 12. Gerçek mesafeye göre nihai ücreti hesapla.
        // Başlangıçta kaydedilen şehir tarifesi kullanılır.
        BigDecimal finalAmount = actualDistanceKm
                .multiply(tripFare.getPricePerKm())
                .setScale(2, RoundingMode.HALF_UP);

        // Mevcut ücret kaydını güncelle; ikinci bir TripFare oluşturma.
        tripFare.setDistanceKm(actualDistanceKm);
        tripFare.setTotalAmount(finalAmount);
        tripFare.setStatus(TripFareStatus.FINALIZED);

        // 13. Tahsilat yap.
        // Bu işlem aynı transaction içinde olmalı.
        // Tahsilat başarısız olursa tamamlanma işlemleri de geri alınır.
        walletService.chargeTrip(userId, trip, finalAmount);

        // 14. Seyahat özetini güncelle
        trip.setActualDistanceKm(actualDistanceKm);
        trip.setActualDurationMinutes(actualDurationMinutes);
        trip.setCompletedAt(completedAt);
        trip.setStatus(TripStatus.COMPLETED);

        // 15. Aracın son konumunu ve menzil bilgilerini güncelle
        Vehicle vehicle = trip.getVehicle();

        vehicle.setCurrentLatitude(request.getLatitude());
        vehicle.setCurrentLongitude(request.getLongitude());
        vehicle.setLastLocationAt(completedAt);

        if (request.getCityName() != null
                && !request.getCityName().isBlank()) {
            vehicle.setCurrentCity(request.getCityName().trim());
        }

        // İsteğin doğrultusunda toplam menzil değerini artır.
        if (vehicle.getTotalRangeKm() != null) {
            vehicle.setTotalRangeKm(
                    vehicle.getTotalRangeKm()
                            .add(actualDistanceKm)
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }

        // Kalan menzili gidilen mesafe kadar azalt.
        if (vehicle.getRemainingRangeKm() != null) {
            BigDecimal remainingRange = vehicle.getRemainingRangeKm()
                    .subtract(actualDistanceKm);

            vehicle.setRemainingRangeKm(
                    remainingRange.max(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }

        // Arıza nedeniyle bakımda olan araç bakımda kalmalı.
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }

        // 16. Değişiklikleri kaydet
        tripFareRepository.save(tripFare);
        tripRepository.save(trip);
        vehicleRepository.save(vehicle);


        // 17. İstemciye kapsamlı seyahat özetini döndür
        TripCompleteResponse response = new TripCompleteResponse();

        response.setTripId(trip.getId());
        response.setStatus(trip.getStatus());

        response.setOriginCity(trip.getOriginCity());
        response.setDestinationCity(trip.getDestinationCity());

        response.setEstimatedDistanceKm(trip.getEstimatedDistanceKm());
        response.setActualDistanceKm(actualDistanceKm);
        response.setDistanceDifferenceKm(
                actualDistanceKm.subtract(trip.getEstimatedDistanceKm())
                        .setScale(2, RoundingMode.HALF_UP)
        );

        response.setEstimatedDurationMinutes(
                trip.getEstimatedDurationMinutes()
        );
        response.setActualDurationMinutes(actualDurationMinutes);
        response.setDurationDifferenceMinutes(
                actualDurationMinutes - trip.getEstimatedDurationMinutes()
        );

        response.setFinalAmount(finalAmount);
        response.setCurrency(tripFare.getCurrency());

        response.setVehicleStatus(vehicle.getStatus());
        response.setVehicleRemainingRangeKm(
                vehicle.getRemainingRangeKm()
        );

        response.setCompletedAt(completedAt);

        return response;
    }
}
