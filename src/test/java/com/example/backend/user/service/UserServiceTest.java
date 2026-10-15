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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Testleri")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(activeUser, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        activeUser.setFullName("Test Kullanici");
        activeUser.setPhone("5551234567");
        activeUser.setPasswordHash("hashed-password");
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setPhoneVerified(true);
    }

    // =========================================================================
    // register
    // =========================================================================

    @Test
    @DisplayName("register - başarılı: kullanıcı PENDING olarak kaydedilmeli ve OTP oluşturulmalı")
    void register_success_savesPendingUserAndOtp() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setPhone("5559876543");
        request.setFullName("Yeni Kullanici");
        request.setIdentityNumber("12345678901");
        request.setPassword("GucluSifre123");

        when(userRepository.existsByPhone("5559876543")).thenReturn(false);
        when(userRepository.existsByIdentityNumber("12345678901")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pw");

        User savedUser = new User();
        savedUser.setPhone("5559876543");
        savedUser.setStatus(UserStatus.PENDING);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(savedUser, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));

        // OTP kaydedilmeli
        ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(otpCaptor.capture());
        OtpCode savedOtp = otpCaptor.getValue();
        assertThat(savedOtp.getCodeHash()).isNotNull();
        assertThat(savedOtp.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));
    }

    @Test
    @DisplayName("register - telefon zaten kayıtlıysa IllegalArgumentException fırlatmalı")
    void register_duplicatePhone_throwsException() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setPhone("5551234567");
        request.setFullName("X");
        request.setIdentityNumber("11111111111");
        request.setPassword("Sifre1234");

        when(userRepository.existsByPhone("5551234567")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("register - kimlik numarası zaten kayıtlıysa IllegalArgumentException fırlatmalı")
    void register_duplicateIdentityNumber_throwsException() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setPhone("5559999999");
        request.setFullName("Y");
        request.setIdentityNumber("12345678901");
        request.setPassword("Sifre1234");

        when(userRepository.existsByPhone("5559999999")).thenReturn(false);
        when(userRepository.existsByIdentityNumber("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("register - şifre hashlenerek kaydedilmeli (plain text olmamalı)")
    void register_passwordIsHashed() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setPhone("5552223344");
        request.setFullName("Z");
        request.setIdentityNumber("99887766554");
        request.setPassword("PlainPassword1");

        when(userRepository.existsByPhone(any())).thenReturn(false);
        when(userRepository.existsByIdentityNumber(any())).thenReturn(false);
        when(passwordEncoder.encode("PlainPassword1")).thenReturn("$bcrypt$hash");

        User savedUser = new User();
        savedUser.setPhone("5552223344");
        savedUser.setStatus(UserStatus.PENDING);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(savedUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$bcrypt$hash");
    }

    // =========================================================================
    // verifyOtp
    // =========================================================================

    @Test
    @DisplayName("verifyOtp - kullanıcı bulunamazsa IllegalArgumentException fırlatmalı")
    void verifyOtp_userNotFound_throwsException() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5550000001");
        request.setCode("123456");

        when(userRepository.findByPhone("5550000001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("verifyOtp - kullanıcı zaten doğrulanmışsa IllegalArgumentException fırlatmalı")
    void verifyOtp_alreadyVerified_throwsException() {
        activeUser.setPhoneVerified(true);
        activeUser.setStatus(UserStatus.ACTIVE);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5551234567");
        request.setCode("123456");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("verifyOtp - kullanıcı durumu PENDING değilse IllegalArgumentException fırlatmalı")
    void verifyOtp_statusNotPending_throwsException() {
        User blockedUser = new User();
        blockedUser.setPhoneVerified(false);
        blockedUser.setStatus(UserStatus.BLOCKED);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5550000002");
        request.setCode("123456");

        when(userRepository.findByPhone("5550000002")).thenReturn(Optional.of(blockedUser));

        assertThatThrownBy(() -> userService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("verifyOtp - geçerli OTP yoksa IllegalArgumentException fırlatmalı")
    void verifyOtp_noValidOtp_throwsException() {
        User pendingUser = new User();
        pendingUser.setPhoneVerified(false);
        pendingUser.setStatus(UserStatus.PENDING);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(pendingUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5550000003");
        request.setCode("123456");

        when(userRepository.findByPhone("5550000003")).thenReturn(Optional.of(pendingUser));
        when(otpCodeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("verifyOtp - OTP süresi dolmuşsa IllegalArgumentException fırlatmalı")
    void verifyOtp_expiredOtp_throwsException() {
        User pendingUser = new User();
        pendingUser.setPhoneVerified(false);
        pendingUser.setStatus(UserStatus.PENDING);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(pendingUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OtpCode expiredOtp = new OtpCode();
        expiredOtp.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        expiredOtp.setCodeHash("hashed");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5550000004");
        request.setCode("123456");

        when(userRepository.findByPhone("5550000004")).thenReturn(Optional.of(pendingUser));
        when(otpCodeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(expiredOtp));

        assertThatThrownBy(() -> userService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("verifyOtp - OTP kodu yanlışsa IllegalArgumentException fırlatmalı")
    void verifyOtp_wrongCode_throwsException() {
        User pendingUser = new User();
        pendingUser.setPhoneVerified(false);
        pendingUser.setStatus(UserStatus.PENDING);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(pendingUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OtpCode otp = new OtpCode();
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        otp.setCodeHash("hashed-correct-code");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5550000005");
        request.setCode("000000");

        when(userRepository.findByPhone("5550000005")).thenReturn(Optional.of(pendingUser));
        when(otpCodeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("000000", "hashed-correct-code")).thenReturn(false);

        assertThatThrownBy(() -> userService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("verifyOtp - başarılı: kullanıcı ACTIVE ve doğrulanmış olmalı, OTP harcanmış olmalı, token dönmeli")
    void verifyOtp_success_activatesUser_consumesOtp_returnsToken() {
        User pendingUser = new User();
        pendingUser.setPhoneVerified(false);
        pendingUser.setStatus(UserStatus.PENDING);
        pendingUser.setFullName("Yeni Kullanici");
        pendingUser.setPhone("5550000006");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(pendingUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OtpCode otp = new OtpCode();
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        otp.setCodeHash("hashed-code");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhone("5550000006");
        request.setCode("123456");

        when(userRepository.findByPhone("5550000006")).thenReturn(Optional.of(pendingUser));
        when(otpCodeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponse result = userService.verifyOtp(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt-token");
        assertThat(pendingUser.isPhoneVerified()).isTrue();
        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(otp.getConsumedAt()).isNotNull();
    }

    // =========================================================================
    // login
    // =========================================================================

    @Test
    @DisplayName("login - kullanıcı bulunamazsa IllegalArgumentException fırlatmalı")
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setPhone("5550000099");
        request.setPassword("Sifre1234");

        when(userRepository.findByPhone("5550000099")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("login - şifre hash'i null ise IllegalArgumentException fırlatmalı")
    void login_nullPasswordHash_throwsException() {
        activeUser.setPasswordHash(null);

        LoginRequest request = new LoginRequest();
        request.setPhone("5551234567");
        request.setPassword("Sifre1234");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("login - şifre yanlışsa IllegalArgumentException fırlatmalı")
    void login_wrongPassword_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setPhone("5551234567");
        request.setPassword("YanlisKelime");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("YanlisKelime", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("login - kullanıcı aktif değilse IllegalArgumentException fırlatmalı")
    void login_inactiveUser_throwsException() {
        activeUser.setStatus(UserStatus.PENDING);
        activeUser.setPhoneVerified(false);

        LoginRequest request = new LoginRequest();
        request.setPhone("5551234567");
        request.setPassword("DogruSifre");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("DogruSifre", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("login - telefon doğrulanmamışsa IllegalArgumentException fırlatmalı")
    void login_phoneNotVerified_throwsException() {
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setPhoneVerified(false);

        LoginRequest request = new LoginRequest();
        request.setPhone("5551234567");
        request.setPassword("DogruSifre");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("DogruSifre", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("login - başarılı giriş: token ve user info dönmeli")
    void login_success_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setPhone("5551234567");
        request.setPassword("DogruSifre");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("DogruSifre", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("valid-jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponse result = userService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("valid-jwt");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getPhone()).isEqualTo("5551234567");
    }

    @Test
    @DisplayName("login - girişte OTP üretilmemeli")
    void login_doesNotGenerateOtp() {
        LoginRequest request = new LoginRequest();
        request.setPhone("5551234567");
        request.setPassword("DogruSifre");

        when(userRepository.findByPhone("5551234567")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("DogruSifre", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("valid-jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        userService.login(request);

        // OTP repo'ya hiç dokunmamalı
        verifyNoInteractions(otpCodeRepository);
    }
}
