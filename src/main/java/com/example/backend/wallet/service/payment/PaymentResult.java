package com.example.backend.wallet.service.payment;

public record PaymentResult(
        boolean successful,
        String providerReference
) {
}