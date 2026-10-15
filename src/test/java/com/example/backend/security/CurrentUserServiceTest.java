package com.example.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("CurrentUserService Unit Testleri")
class CurrentUserServiceTest {

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authentication null ise IllegalStateException fırlatmalı")
    void getUserId_nullAuthentication_throwsIllegalStateException() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> currentUserService.getUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kimliği doğrulanmış kullanıcı bulunamadı.");
    }

    @Test
    @DisplayName("Authentication isAuthenticated false ise IllegalStateException fırlatmalı")
    void getUserId_notAuthenticated_throwsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> currentUserService.getUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kimliği doğrulanmış kullanıcı bulunamadı.");
    }

    @Test
    @DisplayName("Principal String tipinde değilse IllegalStateException fırlatmalı")
    void getUserId_principalNotString_throwsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(12345); // Integer principal

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> currentUserService.getUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kimliği doğrulanmış kullanıcı bulunamadı.");
    }

    @Test
    @DisplayName("Principal geçerli bir UUID değilse IllegalArgumentException fırlatmalı")
    void getUserId_principalInvalidUuid_throwsIllegalArgumentException() {
        Authentication auth = new TestingAuthenticationToken("not-a-valid-uuid", null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> currentUserService.getUserId())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Başarılı durum - Geçerli authenticated UUID ile userId dönmeli")
    void getUserId_validAuthenticatedUser_returnsUuid() {
        UUID expectedUserId = UUID.randomUUID();
        Authentication auth = new TestingAuthenticationToken(expectedUserId.toString(), null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID actualUserId = currentUserService.getUserId();

        assertThat(actualUserId).isEqualTo(expectedUserId);
    }
}
