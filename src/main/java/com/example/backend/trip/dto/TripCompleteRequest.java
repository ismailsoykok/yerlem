
package com.example.backend.trip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class TripCompleteRequest {

    @NotNull(message = "Enlem zorunludur")
    @DecimalMin(value = "-90.0", message = "Enlem -90 ile 90 arasında olmalıdır")
    @DecimalMax(value = "90.0", message = "Enlem -90 ile 90 arasında olmalıdır")
    private BigDecimal latitude;

    @NotNull(message = "Boylam zorunludur")
    @DecimalMin(value = "-180.0", message = "Boylam -180 ile 180 arasında olmalıdır")
    @DecimalMax(value = "180.0", message = "Boylam -180 ile 180 arasında olmalıdır")
    private BigDecimal longitude;

    private String cityName;

    @PositiveOrZero(message = "Hız negatif olamaz")
    private BigDecimal speedKmh;

    public TripCompleteRequest() {
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public BigDecimal getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(BigDecimal speedKmh) {
        this.speedKmh = speedKmh;
    }
}
