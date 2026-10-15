package com.example.backend.wallet.service.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestPaymentProvider Unit Testleri")
class TestPaymentProviderTest {

    private final TestPaymentProvider provider = new TestPaymentProvider();

    @Test
    @DisplayName("processTopUp başarılı sonuç ve TEST- ile başlayan referans dönmeli")
    void processTopUp_returnsSuccessAndReference() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        PaymentResult result = provider.processTopUp(userId, amount);

        assertThat(result).isNotNull();
        assertThat(result.successful()).isTrue();
        assertThat(result.providerReference()).isNotNull().startsWith("TEST-");
    }

    @Test
    @DisplayName("processTopUp her çağrıda benzersiz referans üretmeli")
    void processTopUp_generatesUniqueReferences() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        PaymentResult result1 = provider.processTopUp(userId, amount);
        PaymentResult result2 = provider.processTopUp(userId, amount);

        assertThat(result1.providerReference()).isNotEqualTo(result2.providerReference());
    }
}
