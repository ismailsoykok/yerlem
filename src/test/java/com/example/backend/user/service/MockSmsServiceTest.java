package com.example.backend.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("MockSmsService Unit Testleri")
class MockSmsServiceTest {

    private final MockSmsService mockSmsService = new MockSmsService();

    @Test
    @DisplayName("sendOtp çağrısı başarıyla log üretmeli ve istisna fırlatmamalı")
    void sendOtp_successfulCall_doesNotThrow() {
        assertThatCode(() -> mockSmsService.sendOtp("5551234567", "123456"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendOtp çağrısı parametreler null iken de patlamamalı")
    void sendOtp_nullParameters_doesNotThrow() {
        assertThatCode(() -> mockSmsService.sendOtp(null, null))
                .doesNotThrowAnyException();
    }
}
