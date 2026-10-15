package com.example.backend.wallet.service;

import com.example.backend.trip.entity.Trip;
import com.example.backend.wallet.dto.WalletTopUpResponse;
import com.example.backend.wallet.entity.Wallet;
import com.example.backend.wallet.entity.WalletMovement;
import com.example.backend.wallet.entity.WalletMovementType;
import com.example.backend.wallet.repository.WalletMovementRepository;
import com.example.backend.wallet.repository.WalletRepository;
import com.example.backend.wallet.service.payment.PaymentProvider;
import com.example.backend.wallet.service.payment.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Testleri")
class WalletServiceTest {

    @Mock
    private PaymentProvider paymentProvider;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMovementRepository walletMovementRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setBalance(new BigDecimal("500.00"));
        wallet.setCurrency("TRY");
    }

    // =========================================================================
    // checkSufficientBalance
    // =========================================================================

    @Test
    @DisplayName("checkSufficientBalance - tutar null ise BAD_REQUEST fırlatmalı")
    void checkSufficientBalance_nullAmount_throws400() {
        assertThatThrownBy(() -> walletService.checkSufficientBalance(userId, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("checkSufficientBalance - tutar sıfır ise BAD_REQUEST fırlatmalı")
    void checkSufficientBalance_zeroAmount_throws400() {
        assertThatThrownBy(() -> walletService.checkSufficientBalance(userId, BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("checkSufficientBalance - tutar negatif ise BAD_REQUEST fırlatmalı")
    void checkSufficientBalance_negativeAmount_throws400() {
        assertThatThrownBy(() -> walletService.checkSufficientBalance(userId, new BigDecimal("-10")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("checkSufficientBalance - cüzdan bulunamazsa 404 fırlatmalı")
    void checkSufficientBalance_walletNotFound_throws404() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.checkSufficientBalance(userId, new BigDecimal("100")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("checkSufficientBalance - bakiye yetersizse CONFLICT fırlatmalı")
    void checkSufficientBalance_insufficientBalance_throwsConflict() {
        wallet.setBalance(new BigDecimal("50.00"));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.checkSufficientBalance(userId, new BigDecimal("100.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("checkSufficientBalance - bakiye tam eşit ise sorunsuz geçmeli")
    void checkSufficientBalance_exactBalance_passes() {
        wallet.setBalance(new BigDecimal("100.00"));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // exception fırlatmamalı
        walletService.checkSufficientBalance(userId, new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("checkSufficientBalance - bakiye yeterliyse sorunsuz geçmeli")
    void checkSufficientBalance_sufficientBalance_passes() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        walletService.checkSufficientBalance(userId, new BigDecimal("100.00"));
        // exception fırlatmamalı
        verify(walletRepository, times(1)).findByUserId(userId);
    }

    // =========================================================================
    // chargeTrip
    // =========================================================================

    @Test
    @DisplayName("chargeTrip - tutar null ise BAD_REQUEST fırlatmalı")
    void chargeTrip_nullAmount_throws400() {
        Trip trip = new Trip();
        assertThatThrownBy(() -> walletService.chargeTrip(userId, trip, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("chargeTrip - trip null ise BAD_REQUEST fırlatmalı")
    void chargeTrip_nullTrip_throws400() {
        assertThatThrownBy(() -> walletService.chargeTrip(userId, null, new BigDecimal("50")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("chargeTrip - idempotency: aynı seyahat için daha önce tahsilat yapılmışsa tekrar yapılmamalı")
    void chargeTrip_idempotency_returnsExisting() {
        UUID tripId = UUID.randomUUID();
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(tripId);

        WalletMovement existingMovement = new WalletMovement();
        String key = "TRIP_CHARGE:" + tripId;
        when(walletMovementRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingMovement));

        WalletMovement result = walletService.chargeTrip(userId, trip, new BigDecimal("50"));

        assertThat(result).isSameAs(existingMovement);
        verify(walletRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    @DisplayName("chargeTrip - cüzdan bulunamazsa 404 fırlatmalı")
    void chargeTrip_walletNotFound_throws404() {
        UUID tripId = UUID.randomUUID();
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(tripId);
        when(walletMovementRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.chargeTrip(userId, trip, new BigDecimal("50")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("chargeTrip - bakiye yetersizse CONFLICT fırlatmalı")
    void chargeTrip_insufficientBalance_throwsConflict() {
        UUID tripId = UUID.randomUUID();
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(tripId);

        wallet.setBalance(new BigDecimal("20.00"));
        when(walletMovementRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.chargeTrip(userId, trip, new BigDecimal("50.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("chargeTrip - başarılı tahsilat: bakiye düşmeli ve movement kaydedilmeli")
    void chargeTrip_success_deductsBalance_savesMovement() {
        UUID tripId = UUID.randomUUID();
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(tripId);

        wallet.setBalance(new BigDecimal("500.00"));
        when(walletMovementRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        WalletMovement savedMovement = new WalletMovement();
        when(walletMovementRepository.save(any(WalletMovement.class))).thenReturn(savedMovement);

        WalletMovement result = walletService.chargeTrip(userId, trip, new BigDecimal("100.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        verify(walletRepository).save(wallet);
        assertThat(result).isSameAs(savedMovement);

        // kaydedilen movement'ın alanlarını kontrol et
        ArgumentCaptor<WalletMovement> captor = ArgumentCaptor.forClass(WalletMovement.class);
        verify(walletMovementRepository).save(captor.capture());
        WalletMovement captured = captor.getValue();
        assertThat(captured.getMovementType()).isEqualTo(WalletMovementType.TRIP_CHARGE);
        assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(captured.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(captured.getIdempotencyKey()).isEqualTo("TRIP_CHARGE:" + tripId);
    }

    // =========================================================================
    // topUp
    // =========================================================================

    @Test
    @DisplayName("topUp - tutar null ise BAD_REQUEST fırlatmalı")
    void topUp_nullAmount_throws400() {
        assertThatThrownBy(() -> walletService.topUp(userId, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("topUp - tutar sıfır ise BAD_REQUEST fırlatmalı")
    void topUp_zeroAmount_throws400() {
        assertThatThrownBy(() -> walletService.topUp(userId, BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("topUp - 2'den fazla ondalık basamak ise BAD_REQUEST fırlatmalı")
    void topUp_tooManyDecimalPlaces_throws400() {
        // 100.001 has scale=3
        assertThatThrownBy(() -> walletService.topUp(userId, new BigDecimal("100.001")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("topUp - cüzdan bulunamazsa 404 fırlatmalı")
    void topUp_walletNotFound_throws404() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.topUp(userId, new BigDecimal("100.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("topUp - ödeme sağlayıcı null dönerse PAYMENT_REQUIRED fırlatmalı")
    void topUp_paymentProviderReturnsNull_throws402() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(paymentProvider.processTopUp(userId, new BigDecimal("100.00"))).thenReturn(null);

        assertThatThrownBy(() -> walletService.topUp(userId, new BigDecimal("100.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.PAYMENT_REQUIRED));
    }

    @Test
    @DisplayName("topUp - ödeme sağlayıcı başarısız dönerse PAYMENT_REQUIRED fırlatmalı")
    void topUp_paymentFailed_throws402() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        PaymentResult failedResult = new PaymentResult(false, "FAILED-REF");
        when(paymentProvider.processTopUp(userId, new BigDecimal("50.00"))).thenReturn(failedResult);

        assertThatThrownBy(() -> walletService.topUp(userId, new BigDecimal("50.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.PAYMENT_REQUIRED));
    }

    @Test
    @DisplayName("topUp - idempotency: aynı referansla tekrar yükleme mevcut movement'ı dönmeli")
    void topUp_idempotency_returnsExistingMovement() {
        String ref = "REF-123";
        String idempKey = "TOP_UP:" + ref;

        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        PaymentResult successResult = new PaymentResult(true, ref);
        when(paymentProvider.processTopUp(userId, new BigDecimal("100.00"))).thenReturn(successResult);

        WalletMovement existingMv = new WalletMovement();
        existingMv.setAmount(new BigDecimal("100.00"));
        existingMv.setBalanceAfter(new BigDecimal("600.00"));

        when(walletMovementRepository.findByIdempotencyKey(idempKey)).thenReturn(Optional.of(existingMv));

        WalletTopUpResponse result = walletService.topUp(userId, new BigDecimal("100.00"));

        assertThat(result).isNotNull();
        assertThat(result.getPaymentReference()).isEqualTo(ref);
        // bakiye tekrar artırılmamalı
        verify(walletRepository, never()).save(any());
        verify(walletMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("topUp - başarılı yükleme: bakiye artmalı, movement kaydedilmeli, response dönmeli")
    void topUp_success_updatesBalance_savesMovement_returnsResponse() {
        String ref = "TEST-REF-456";
        String idempKey = "TOP_UP:" + ref;

        wallet.setBalance(new BigDecimal("500.00"));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        PaymentResult successResult = new PaymentResult(true, ref);
        when(paymentProvider.processTopUp(userId, new BigDecimal("200.00"))).thenReturn(successResult);
        when(walletMovementRepository.findByIdempotencyKey(idempKey)).thenReturn(Optional.empty());

        WalletMovement savedMv = new WalletMovement();
        savedMv.setAmount(new BigDecimal("200.00"));
        savedMv.setBalanceAfter(new BigDecimal("700.00"));
        when(walletMovementRepository.save(any(WalletMovement.class))).thenReturn(savedMv);

        WalletTopUpResponse result = walletService.topUp(userId, new BigDecimal("200.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        verify(walletRepository).save(wallet);
        assertThat(result).isNotNull();
        assertThat(result.getPaymentReference()).isEqualTo(ref);

        // movement türünü doğrula
        ArgumentCaptor<WalletMovement> captor = ArgumentCaptor.forClass(WalletMovement.class);
        verify(walletMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(WalletMovementType.TOP_UP);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(idempKey);
    }

    @Test
    @DisplayName("topUp - 2 ondalık basamak (100.00) kabul edilmeli")
    void topUp_twoDecimalPlaces_accepted() {
        String ref = "REF-TWO-DEC";
        wallet.setBalance(new BigDecimal("100.00"));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(paymentProvider.processTopUp(userId, new BigDecimal("50.00"))).thenReturn(new PaymentResult(true, ref));
        when(walletMovementRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        WalletMovement mv = new WalletMovement();
        mv.setAmount(new BigDecimal("50.00"));
        mv.setBalanceAfter(new BigDecimal("150.00"));
        when(walletMovementRepository.save(any())).thenReturn(mv);

        // exception fırlatmamalı
        WalletTopUpResponse response = walletService.topUp(userId, new BigDecimal("50.00"));
        assertThat(response).isNotNull();
    }
}
