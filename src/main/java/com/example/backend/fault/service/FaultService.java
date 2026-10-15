package com.example.backend.fault.service;

import com.example.backend.alert.entity.TripAlert;
import com.example.backend.alert.service.AlertService;
import com.example.backend.fault.dto.FaultReportRequest;
import com.example.backend.fault.dto.FaultReportResponse;
import com.example.backend.fault.entity.FaultReport;
import com.example.backend.fault.repository.FaultReportRepository;
import com.example.backend.security.CurrentUserService;
import com.example.backend.trip.entity.Trip;
import com.example.backend.trip.entity.TripStatus;
import com.example.backend.trip.repository.TripRepository;
import com.example.backend.vehicle.entity.Vehicle;
import com.example.backend.vehicle.entity.VehicleStatus;
import com.example.backend.vehicle.repository.VehicleRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class FaultService {

    private final FaultReportRepository faultReportRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;
    private final AlertService alertService;

    public FaultService(
            FaultReportRepository faultReportRepository,
            TripRepository tripRepository,
            VehicleRepository vehicleRepository,
            CurrentUserService currentUserService,
            AlertService alertService
    ) {
        this.faultReportRepository = faultReportRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.currentUserService = currentUserService;
        this.alertService = alertService;
    }

    @Transactional
    public void reportFault(UUID tripId, FaultReportRequest request) {

        if (request == null || request.getFaultType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Arıza türü belirtilmelidir."
            );
        }

        UUID userId = currentUserService.getUserId();

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahat için arıza bildiremezsiniz."
            );
        }

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Yalnızca devam eden seyahatlerde arıza bildirilebilir."
            );
        }

        Vehicle vehicle = trip.getVehicle();

        FaultReport faultReport = new FaultReport();
        faultReport.setTrip(trip);
        faultReport.setVehicle(vehicle);
        faultReport.setReportedByUser(trip.getUser());
        faultReport.setFaultType(request.getFaultType());

        String description = request.getDescription();

        if (description != null && !description.isBlank()) {
            faultReport.setDescription(description.trim());
        }

        faultReportRepository.save(faultReport);

        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.save(vehicle);

        String message = "Araç arızası bildirildi: "
                + request.getFaultType();

        if (description != null && !description.isBlank()) {
            message += " - " + description.trim();
        }

        alertService.createAlert(
                tripId,
                TripAlert.FAULT_REPORTED,
                message,
                null,
                null
        );
    }




    @Transactional(readOnly = true)
    public List<FaultReportResponse> getTripFaults(UUID tripId) {

        UUID userId = currentUserService.getUserId();

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seyahat bulunamadı."
                ));

        if (!trip.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu seyahatin arızalarını görüntüleyemezsiniz."
            );
        }

        return faultReportRepository
                .findAllByTripIdOrderByReportedAtDesc(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FaultReportResponse toResponse(FaultReport fault) {

        FaultReportResponse response = new FaultReportResponse();

        response.setId(fault.getId());
        response.setTripId(fault.getTrip().getId());
        response.setVehicleId(fault.getVehicle().getId());
        response.setFaultType(fault.getFaultType());
        response.setDescription(fault.getDescription());
        response.setReportedAt(fault.getReportedAt());
        response.setResolvedAt(fault.getResolvedAt());
        response.setStatus(fault.getStatus());

        return response;
    }

}
