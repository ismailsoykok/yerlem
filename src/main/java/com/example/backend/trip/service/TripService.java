
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

        // önce giriş yapan kullanıcıyı buluyoruz
        UUID userId = currentUserService.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kullanıcı bulunamadı."
                ));

        // kullanıcı aktif mi ve telefonu onaylı mı diye bakıyoruz
        if (user.getStatus() != UserStatus.ACTIVE
                || !user.isPhoneVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seyahat başlatmak için hesabınız aktif ve telefonunuz doğrulanmış olmalıdır."
            );
        }

        // aracı kilitli çekiyoruz ki aynı anda başkası alamasın
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

        // client'tan gelen ücrete güvenmeyip sunucuda tekrar hesaplıyoruz
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

        // tahmini ücret için yeterli bakiye var mı diye kontrol ediyoruz

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

        // seyahat kaydını oluşturuyoruz
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

        // seyahatin ücret detaylarını ayrı tabloda tutuyoruz
        TripFare tripFare = new TripFare();
        tripFare.setTrip(trip);
        tripFare.setCityFare(cityFare);
        tripFare.setDistanceKm(estimate.getDistanceKm());
        tripFare.setPricePerKm(estimate.getPricePerKm());
        tripFare.setTotalAmount(estimate.getEstimatedFee());
        tripFare.setCurrency(estimate.getCurrency());
        tripFare.setStatus(TripFareStatus.ESTIMATED);

        tripFareRepository.save(tripFare);

        // aracı artık kullanımda olarak işaretliyoruz
        vehicle.setStatus(VehicleStatus.IN_USE);
        vehicleRepository.save(vehicle);

        // seyahat bilgilerini istemciye dönüyoruz
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

        // gelen isteği ve koordinatları doğruluyoruz
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

        // giriş yapan kullanıcıyı alıyoruz
        UUID userId = currentUserService.getUserId();

        // seyahat kaydını buluyoruz
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        // bu seyahat gerçekten bu kullanıcıya mı ait ona bakıyoruz
        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahate konum gönderme yetkiniz yok."
            );
        }

        // seyahat hala devam ediyor mu diye kontrol ediyoruz
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Yalnızca devam eden seyahatlere konum eklenebilir."
            );
        }

        // yeni konumu kaydediyoruz
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

        // hız sınırını aşıp aşmadığına bakıyoruz
        if (speedKmh != null && request.getCityName() != null) {

            String cityName = request.getCityName().trim();

            // başlangıç şehriyle aynı yerdeyse şehir içi sayıyoruz
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

        // kalan menzili kontrol ediyoruz
        BigDecimal actualDistanceKm =
                postgisTripDistanceRepository
                        .calculateActualDistanceKm(trip.getId());

        if (actualDistanceKm != null) {

            BigDecimal remainingRangeKm =
                    trip.getVehicle().getRemainingRangeKm();

            BigDecimal estimatedRemainingRange =
                    remainingRangeKm.subtract(actualDistanceKm);

            // menzil 1 km altına düşerse yolculuğu direkt bitiriyoruz
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
        // gelen istek ve son koordinatları kontrol ediyoruz
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

        // jwt üzerinden aktif kullanıcıyı alıyoruz
        UUID userId = currentUserService.getUserId();

        // seyahati kilitli getiriyoruz ki aynı anda iki defa bitirme isteği gelirse çakışmasın
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        // seyahatin bu kullanıcıya ait olup olmadığını kontrol ediyoruz
        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahati tamamlama yetkiniz bulunmuyor."
            );
        }

        // sadece devam eden seyahatler bitirilebilir o yüzden durumuna bakıyoruz
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

        // son konumu da zaman çizelgesine ekliyoruz
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

        // hız sınırını aştıysa hemen uyarı kaydı oluşturuyoruz
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

        // konum kayıtları üstünden gidilen gerçek mesafeyi hesaplıyoruz
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

        // seyahatin toplam kaç dakika sürdüğünü hesaplıyoruz
        long durationMinutes = Duration.between(
                trip.getStartedAt(),
                completedAt
        ).toMinutes();

        int actualDurationMinutes = Math.toIntExact(durationMinutes);

        // tahmini süreyle gerçek süreyi kıyaslıyoruz yüzde yirmiyi geçerse uyarı atacağız
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

        // seyahat ücret kaydını kontrol ediyoruz
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

        // gidilen gerçek mesafeye göre başlangıçtaki tarifeden son ücreti hesaplıyoruz
        BigDecimal finalAmount = actualDistanceKm
                .multiply(tripFare.getPricePerKm())
                .setScale(2, RoundingMode.HALF_UP);

        // yeni kayıt açmak yerine mevcut ücret kaydını güncelliyoruz
        tripFare.setDistanceKm(actualDistanceKm);
        tripFare.setTotalAmount(finalAmount);
        tripFare.setStatus(TripFareStatus.FINALIZED);

        // tahsilatı yapıyoruz işlem patlarsa rollback olsun diye aynı transaction içinde tutuyoruz
        walletService.chargeTrip(userId, trip, finalAmount);

        // seyahat bilgilerini ve durumunu tamamlandı olarak güncelliyoruz
        trip.setActualDistanceKm(actualDistanceKm);
        trip.setActualDurationMinutes(actualDurationMinutes);
        trip.setCompletedAt(completedAt);
        trip.setStatus(TripStatus.COMPLETED);

        // aracın son konumunu ve menzilini güncelliyoruz
        Vehicle vehicle = trip.getVehicle();

        vehicle.setCurrentLatitude(request.getLatitude());
        vehicle.setCurrentLongitude(request.getLongitude());
        vehicle.setLastLocationAt(completedAt);

        if (request.getCityName() != null
                && !request.getCityName().isBlank()) {
            vehicle.setCurrentCity(request.getCityName().trim());
        }

        // toplam yapılan mesafeyi aracın kilometresine ekliyoruz
        if (vehicle.getTotalRangeKm() != null) {
            vehicle.setTotalRangeKm(
                    vehicle.getTotalRangeKm()
                            .add(actualDistanceKm)
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }

        // gittiği mesafe kadar kalan menzili düşürüyoruz
        if (vehicle.getRemainingRangeKm() != null) {
            BigDecimal remainingRange = vehicle.getRemainingRangeKm()
                    .subtract(actualDistanceKm);

            vehicle.setRemainingRangeKm(
                    remainingRange.max(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }

        // araç zaten arıza yüzünden bakımdaysa durumunu bozmuyoruz
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }

        // yapılan değişiklikleri veritabanına kaydediyoruz
        tripFareRepository.save(tripFare);
        tripRepository.save(trip);
        vehicleRepository.save(vehicle);


        // son seyahat özetini kullanıcıya dönüyoruz
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
