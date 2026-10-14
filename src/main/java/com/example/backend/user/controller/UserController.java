
package com.example.backend.user.controller;

import com.example.backend.user.dto.AuthResponse;
import com.example.backend.user.dto.LoginRequest;
import com.example.backend.user.dto.UserRegisterRequest;
import com.example.backend.user.dto.VerifyOtpRequest;
import com.example.backend.user.entity.User;
import com.example.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody UserRegisterRequest request) {

        User user = userService.register(request);

        Map<String, Object> response = Map.of(
                "id", user.getId().toString(),
                "fullName", user.getFullName(),
                "phone", user.getPhone(),
                "status", user.getStatus().name(),
                "message",
                "Kayıt oluşturuldu. OTP kodunu girerek telefonunu doğrula."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        return ResponseEntity.ok(
                userService.verifyOtp(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                userService.login(request)
        );
    }
}
