package com.example.backend.alert.service;

import com.example.backend.alert.dto.TripAlertResponse;
import com.example.backend.alert.entity.TripAlert;
import com.example.backend.alert.entity.TripAlertEntity;
import com.example.backend.alert.repository.TripAlertRepository;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final TripAlertRepository tripAlertRepository;
    private final TripRepository tripRepository;

    public AlertService(
            TripAlertRepository tripAlertRepository,
            TripRepository tripRepository) {
        this.tripAlertRepository = tripAlertRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public TripAlertResponse createAlert(
            UUID tripId,
            TripAlert alertType,
            String message,
            BigDecimal recordedValue,
            BigDecimal thresholdValue) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        TripAlertEntity alert = new TripAlertEntity();
        alert.setTrip(trip);
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setRecordedValue(recordedValue);
        alert.setThresholdValue(thresholdValue);

        return toResponse(tripAlertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<TripAlertResponse> getTripAlerts(UUID tripId) {

        if (!tripRepository.existsById(tripId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Seyahat bulunamadı."
            );
        }

        return tripAlertRepository
                .findAllByTripIdOrderByCreatedAtDesc(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TripAlertResponse toResponse(TripAlertEntity alert) {
        TripAlertResponse response = new TripAlertResponse();
        response.setId(alert.getId());
        response.setTripId(alert.getTrip().getId());
        response.setAlertType(alert.getAlertType());
        response.setMessage(alert.getMessage());
        response.setRecordedValue(alert.getRecordedValue());
        response.setThresholdValue(alert.getThresholdValue());
        response.setCreatedAt(alert.getCreatedAt());
        return response;
    }
}
