
package com.example.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegisterRequest {

    @NotBlank(message = "Ad soyad boş olamaz")
    @Size(max = 150, message = "Ad soyad en fazla 150 karakter olabilir")
    private String fullName;

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Telefon numarası 10-15 rakamdan oluşmalıdır"
    )
    private String phone;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank(message = "Kimlik numarası boş olamaz")
    @Pattern(
            regexp = "^[0-9]{11}$",
            message = "Kimlik numarası 11 rakamdan oluşmalıdır"
    )
    private String identityNumber;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }
}
