package com.example.backend.wallet.service.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TestPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processTopUp(UUID userId, BigDecimal amount) {
        // burası sadece test ortamı için var
        // gerçek para çekme falan yok yani
        return new PaymentResult(
                true,
                "TEST-" + UUID.randomUUID()
        );
    }
}