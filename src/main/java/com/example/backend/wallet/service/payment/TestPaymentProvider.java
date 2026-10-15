package com.example.backend.wallet.service.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TestPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processTopUp(UUID userId, BigDecimal amount) {
        // Yalnızca geliştirme/test ortamı.
        // Gerçek para tahsilatı yapılmaz.
        return new PaymentResult(
                true,
                "TEST-" + UUID.randomUUID()
        );
    }
}