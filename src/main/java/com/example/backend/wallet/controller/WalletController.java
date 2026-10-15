package com.example.backend.wallet.controller;

import com.example.backend.wallet.dto.WalletTopUpRequest;
import com.example.backend.wallet.dto.WalletTopUpResponse;
import com.example.backend.wallet.service.WalletService;
import com.example.backend.security.CurrentUserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    public WalletController(
            WalletService walletService,
            CurrentUserService currentUserService) {
        this.walletService = walletService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/top-up")
    @ResponseStatus(HttpStatus.OK)
    public WalletTopUpResponse topUp(
            @Valid @RequestBody WalletTopUpRequest request) {

        return walletService.topUp(
                currentUserService.getUserId(),
                request.amount()
        );
    }
}