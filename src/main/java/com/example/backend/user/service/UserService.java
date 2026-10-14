
package com.example.backend.user.service;

import com.example.backend.user.dto.AuthResponse;
import com.example.backend.user.dto.LoginRequest;
import com.example.backend.user.dto.UserRegisterRequest;
import com.example.backend.user.dto.VerifyOtpRequest;
import com.example.backend.user.entity.OtpCode;
import com.example.backend.user.entity.User;
import com.example.backend.user.entity.UserStatus;
import com.example.backend.user.repository.OtpCodeRepository;
import com.example.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Logger log =
            LoggerFactory.getLogger(UserService.class);

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            OtpCodeRepository otpCodeRepository,
            JwtService jwtService,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(UserRegisterRequest request) {

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException(
                    "Bu telefon numarası zaten kayıtlı."
            );
        }

        if (userRepository.existsByIdentityNumber(
                request.getIdentityNumber())) {
            throw new IllegalArgumentException(
                    "Bu kimlik numarası zaten kayıtlı."
            );
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setIdentityNumber(request.getIdentityNumber());


        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        user.setStatus(UserStatus.PENDING);
        user.setPhoneVerified(false);

        user = userRepository.save(user);


        String code = String.format(
                "%06d",
                SECURE_RANDOM.nextInt(1_000_000)
        );

        OtpCode otp = new OtpCode();
        otp.setUser(user);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setConsumedAt(null);

        otpCodeRepository.save(otp);


        log.info(
                "[MANUEL TEST] Telefon: {}, OTP: {}",
                user.getPhone(),
                code
        );

        return user;
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Telefon numarası veya OTP kodu hatalı."
                ));

        if (user.isPhoneVerified()
                || user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Bu hesap için doğrulama yapılamıyor."
            );
        }

        OtpCode otp = otpCodeRepository
                .findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Geçerli OTP kodu bulunamadı."
                ));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "OTP kodunun süresi dolmuş."
            );
        }

        if (!passwordEncoder.matches(
                request.getCode(),
                otp.getCodeHash())) {
            throw new IllegalArgumentException(
                    "Telefon numarası veya OTP kodu hatalı."
            );
        }

        // OTP tek kullanımlıktır.
        otp.setConsumedAt(LocalDateTime.now());

        user.setPhoneVerified(true);
        user.setStatus(UserStatus.ACTIVE);

        otpCodeRepository.save(otp);
        userRepository.save(user);

        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Telefon numarası veya şifre hatalı."
                ));

        // Daha önce oluşturulmuş, şifresi olmayan hesaplar.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Telefon numarası veya şifre hatalı."
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE
                || !user.isPhoneVerified()) {
            throw new IllegalArgumentException(
                    "Hesap aktif değil. Önce telefon doğrulamasını tamamla."
            );
        }

        // Login sırasında OTP oluşturulmaz.
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {

        String token = jwtService.generateToken(user);

        AuthResponse.UserInfo userInfo =
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getFullName(),
                        user.getPhone(),
                        user.getStatus().name()
                );

        return new AuthResponse(
                token,
                jwtService.getExpirationSeconds(),
                userInfo
        );
    }
}
