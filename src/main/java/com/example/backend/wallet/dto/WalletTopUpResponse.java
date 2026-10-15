package com.example.backend.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletTopUpResponse(
        UUID movementId,
        BigDecimal loadedAmount,
        BigDecimal balance,
        String currency,
        String paymentReference
) {
}