
package com.example.backend.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class MockSmsService implements SmsService {

    private static final Logger log =
            LoggerFactory.getLogger(MockSmsService.class);

    @Override
    public void sendOtp(String phone, String code) {
        log.info("[MANUEL TEST] Telefon: {}, OTP: {}", phone, code);
    }
}
