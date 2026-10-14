
package com.example.backend.user.service;

public interface SmsService {

    void sendOtp(String phone, String code);
}
