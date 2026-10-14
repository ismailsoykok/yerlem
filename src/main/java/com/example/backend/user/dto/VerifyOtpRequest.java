
package com.example.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyOtpRequest {

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Telefon numarası 10-15 rakamdan oluşmalıdır"
    )
    private String phone;

    @NotBlank(message = "OTP kodu boş olamaz")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP kodu 6 rakamdan oluşmalıdır"
    )
    private String code;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
