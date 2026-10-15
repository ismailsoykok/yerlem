package com.example.backend.fault.dto;

import com.example.backend.fault.entity.FaultType;

public class FaultReportRequest {

    private FaultType faultType;
    private String description;

    public FaultReportRequest() {
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public void setFaultType(FaultType faultType) {
        this.faultType = faultType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
