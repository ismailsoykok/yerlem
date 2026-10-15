
package com.example.backend.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletTopUpResponse {

    private UUID movementId;
    private BigDecimal loadedAmount;
    private BigDecimal balance;
    private String currency;
    private String paymentReference;

    public WalletTopUpResponse() {
    }

    public WalletTopUpResponse(
            UUID movementId,
            BigDecimal loadedAmount,
            BigDecimal balance,
            String currency,
            String paymentReference
    ) {
        this.movementId = movementId;
        this.loadedAmount = loadedAmount;
        this.balance = balance;
        this.currency = currency;
        this.paymentReference = paymentReference;
    }

    public UUID getMovementId() {
        return movementId;
    }

    public void setMovementId(UUID movementId) {
        this.movementId = movementId;
    }

    public BigDecimal getLoadedAmount() {
        return loadedAmount;
    }

    public void setLoadedAmount(BigDecimal loadedAmount) {
        this.loadedAmount = loadedAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
}
