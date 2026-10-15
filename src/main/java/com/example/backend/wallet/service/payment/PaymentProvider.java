package com.example.backend.wallet.service.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProvider {

    PaymentResult processTopUp(UUID userId, BigDecimal amount);
}