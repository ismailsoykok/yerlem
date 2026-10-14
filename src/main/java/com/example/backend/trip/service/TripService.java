package com.example.backend.trip.service;

import com.example.backend.fare.entity.CityFare;
import com.example.backend.fare.repository.CityFareRepository;
import com.example.backend.trip.dto.TripEstimateRequest;
import com.example.backend.trip.dto.TripEstimateResponse;
import com.example.backend.trip.repository.PostgisDistanceRepository;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;
import com.example.backend.vehicle.repository.VehicleUsageAreaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TripService {

    private final VehicleRepository vehicleRepository;
    private final VehicleUsageAreaRepository vehicleUsageAreaRepository;
    private final CityFareRepository cityFareRepository;
    private final PostgisDistanceRepository postgisDistanceRepository;

    public TripService(
            VehicleRepository vehicleRepository,
            VehicleUsageAreaRepository vehicleUsageAreaRepository,
            CityFareRepository cityFareRepository,
            PostgisDistanceRepository postgisDistanceRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleUsageAreaRepository = vehicleUsageAreaRepository;
        this.cityFareRepository = cityFareRepository;
        this.postgisDistanceRepository = postgisDistanceRepository;
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
                || vehicle.getRemainingRangeKm().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aracın konum veya menzil bilgileri geçersiz."
            );
        }

        String destinationCity = request.getDestinationCity().trim();

        boolean allowedArea = vehicleUsageAreaRepository
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

        BigDecimal distanceKm = postgisDistanceRepository.calculateDistanceKm(
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


        // Ortalama 50 km/saat varsayımıyla tahmini süre.
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
}