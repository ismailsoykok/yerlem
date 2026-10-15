package com.example.backend.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletTopUpRequest(

        @NotNull(message = "Yükleme tutarı zorunludur.")
        @DecimalMin(
                value = "0.01",
                message = "Yükleme tutarı sıfırdan büyük olmalıdır."
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Tutar en fazla iki ondalık basamak içerebilir."
        )
        BigDecimal amount

) {
}