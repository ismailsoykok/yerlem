package com.example.backend.photo.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class DamageDetectionResponse {

    private UUID tripId;
    private List<DamageChargeResponse> damageCharges;
    private BigDecimal totalExtraAmount;
    private String currency;

    public DamageDetectionResponse() {
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public List<DamageChargeResponse> getDamageCharges() {
        return damageCharges;
    }

    public void setDamageCharges(List<DamageChargeResponse> damageCharges) {
        this.damageCharges = damageCharges;
    }

    public BigDecimal getTotalExtraAmount() {
        return totalExtraAmount;
    }

    public void setTotalExtraAmount(BigDecimal totalExtraAmount) {
        this.totalExtraAmount = totalExtraAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
