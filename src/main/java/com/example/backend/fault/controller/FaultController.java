package com.example.backend.fault.controller;

import com.example.backend.fault.dto.FaultReportRequest;
import com.example.backend.fault.dto.FaultReportResponse;
import com.example.backend.fault.entity.FaultReport;
import com.example.backend.fault.service.FaultService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/faults")
public class FaultController {

    private final FaultService faultService;

    public FaultController(FaultService faultService) {
        this.faultService = faultService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void reportFault(
            @PathVariable UUID tripId,
            @RequestBody FaultReportRequest request) {

        faultService.reportFault(tripId, request);
    }

    @GetMapping
    public List<FaultReportResponse> getTripFaults(
            @PathVariable UUID tripId
    ) {
        return faultService.getTripFaults(tripId);
    }
}
