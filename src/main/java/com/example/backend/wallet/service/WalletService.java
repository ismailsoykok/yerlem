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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final PaymentProvider paymentProvider;

    private final WalletRepository walletRepository;
    private final WalletMovementRepository walletMovementRepository;

    public WalletService(
            @Qualifier("testPaymentProvider") PaymentProvider paymentProvider, WalletRepository walletRepository,
            WalletMovementRepository walletMovementRepository) {
        this.paymentProvider = paymentProvider;
        this.walletRepository = walletRepository;
        this.walletMovementRepository = walletMovementRepository;
    }

    @Transactional(readOnly = true)
    public void checkSufficientBalance(UUID userId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kullanıcının cüzdanı bulunamadı."
                ));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cüzdan bakiyesi seyahat ücretini karşılamıyor."
            );
        }
    }

    @Transactional
    public WalletMovement chargeTrip(
            UUID userId,
            Trip trip,
            BigDecimal amount) {

        validateAmount(amount);

        if (trip == null || trip.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Geçerli bir seyahat belirtilmelidir."
            );
        }

        String idempotencyKey = "TRIP_CHARGE:" + trip.getId();

        var existingMovement =
                walletMovementRepository.findByIdempotencyKey(idempotencyKey);

        if (existingMovement.isPresent()) {
            return existingMovement.get();
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kullanıcının cüzdanı bulunamadı."
                ));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cüzdan bakiyesi seyahat ücretini karşılamıyor."
            );
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletMovement movement = new WalletMovement();
        movement.setWallet(wallet);
        movement.setTrip(trip);
        movement.setMovementType(WalletMovementType.TRIP_CHARGE);
        movement.setAmount(amount);
        movement.setBalanceAfter(newBalance);
        movement.setDescription("Seyahat ücreti tahsilatı");
        movement.setIdempotencyKey(idempotencyKey);

        return walletMovementRepository.save(movement);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "İşlem tutarı sıfırdan büyük olmalıdır."
            );
        }
    }



    @Transactional
    public WalletTopUpResponse topUp(UUID userId, BigDecimal amount) {
        validateAmount(amount);

        if (amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tutar en fazla iki ondalık basamak içerebilir."
            );
        }

        // önce kullanıcının cüzdanını kilitli çekiyoruz
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kullanıcının cüzdanı bulunamadı."
                ));

        // ödeme sağlayıcısından gelen sonuca bakıyoruz
        PaymentResult paymentResult =
                paymentProvider.processTopUp(userId, amount);

        if (paymentResult == null || !paymentResult.successful()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Ödeme işlemi başarısız oldu."
            );
        }

        String idempotencyKey =
                "TOP_UP:" + paymentResult.providerReference();

        // aynı ödeme referansıyla iki kere bakiye yüklenmesin diye kontrol ediyoruz
        var existingMovement =
                walletMovementRepository.findByIdempotencyKey(idempotencyKey);

        if (existingMovement.isPresent()) {
            WalletMovement movement = existingMovement.get();

            return new WalletTopUpResponse(
                    movement.getId(),
                    movement.getAmount(),
                    movement.getBalanceAfter(),
                    wallet.getCurrency(),
                    paymentResult.providerReference()
            );
        }

        // bakiyeyi güncelliyoruz
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);

        walletRepository.save(wallet);

        // cüzdan hareketini kaydediyoruz
        WalletMovement movement = new WalletMovement();
        movement.setWallet(wallet);
        movement.setMovementType(WalletMovementType.TOP_UP);
        movement.setAmount(amount);
        movement.setBalanceAfter(newBalance);
        movement.setDescription("Cüzdana bakiye yükleme");
        movement.setIdempotencyKey(idempotencyKey);

        WalletMovement savedMovement =
                walletMovementRepository.save(movement);

        return new WalletTopUpResponse(
                savedMovement.getId(),
                savedMovement.getAmount(),
                savedMovement.getBalanceAfter(),
                wallet.getCurrency(),
                paymentResult.providerReference()
        );
    }
}