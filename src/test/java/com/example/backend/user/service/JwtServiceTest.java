package com.example.backend.user.service;

import com.example.backend.user.entity.User;
import com.example.backend.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Unit Testleri")
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // 32 karakter (256 bit) secret, HS256 için minimum
        String secret = "testSecretKeyForJwtThatIsLongEnough123456";
        jwtService = new JwtService(secret, 86400L);

        user = new User();
        var field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        UUID userId = UUID.randomUUID();
        field.set(user, userId);
        user.setPhone("5551234567");
        user.setFullName("Test Kullanici");
        user.setStatus(UserStatus.ACTIVE);
        user.setPhoneVerified(true);
    }

    // =========================================================================
    // generateToken
    // =========================================================================

    @Test
    @DisplayName("generateToken - null olmayan token üretmeli")
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(user);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateToken - geçerli JWT formatında (üç parça) token üretmeli")
    void generateToken_returnsValidJwtFormat() {
        String token = jwtService.generateToken(user);

        // JWT 3 parçadan oluşur: header.payload.signature
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }

    @Test
    @DisplayName("generateToken - farklı kullanıcılar için farklı tokenlar üretmeli")
    void generateToken_differentUsers_differentTokens() throws Exception {
        User user2 = new User();
        var field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user2, UUID.randomUUID());
        user2.setPhone("5559999999");

        String token1 = jwtService.generateToken(user);
        String token2 = jwtService.generateToken(user2);

        assertThat(token1).isNotEqualTo(token2);
    }

    // =========================================================================
    // extractUserId
    // =========================================================================

    @Test
    @DisplayName("extractUserId - üretilen tokenden doğru userId çözümlenmeli")
    void extractUserId_correctUserIdExtracted() throws Exception {
        var field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        UUID expectedId = (UUID) field.get(user);

        String token = jwtService.generateToken(user);
        UUID extractedId = jwtService.extractUserId(token);

        assertThat(extractedId).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("extractUserId - geçersiz token verilince exception fırlatmalı")
    void extractUserId_invalidToken_throwsException() {
        assertThatThrownBy(() -> jwtService.extractUserId("invalid.token.value"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("extractUserId - bozulmuş token verilince exception fırlatmalı")
    void extractUserId_tamperedToken_throwsException() {
        String token = jwtService.generateToken(user);
        // token'ı bozuyoruz
        String tamperedToken = token + "tampered";

        assertThatThrownBy(() -> jwtService.extractUserId(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    // =========================================================================
    // getExpirationSeconds
    // =========================================================================

    @Test
    @DisplayName("getExpirationSeconds - constructor'da verilen değeri dönmeli")
    void getExpirationSeconds_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("getExpirationSeconds - farklı expiration ile oluşturulan JwtService doğru değer dönmeli")
    void getExpirationSeconds_customExpiration_returnsCorrectValue() {
        JwtService customService = new JwtService("anotherSecretKeyForTestingPurposesOnly", 3600L);

        assertThat(customService.getExpirationSeconds()).isEqualTo(3600L);
    }
}
