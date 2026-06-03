package com.financeportal.backend.Email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Email Service Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";
    private static final String TEST_EMAIL = "test@test.com";

    private EmailVerificationToken testToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "realm", "finance-portal");

        testToken = EmailVerificationToken.builder()
                .token("test-token-uuid")
                .keycloakId(TEST_USER_ID)
                .email(TEST_EMAIL)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(keycloakAdminClient.realm("finance-portal")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Doğrulama emaili başarıyla gönderilmeli")
    void sendVerificationEmail_Success() {
        doNothing().when(tokenRepository).deleteByKeycloakId(TEST_USER_ID);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
                emailService.sendVerificationEmail(TEST_USER_ID, TEST_EMAIL));

        verify(tokenRepository, times(1)).deleteByKeycloakId(TEST_USER_ID);
        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Geçersiz token ile doğrulama false dönmeli")
    void verifyEmail_TokenNotFound_ReturnsFalse() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        boolean result = emailService.verifyEmail("invalid-token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Kullanılmış token ile doğrulama false dönmeli")
    void verifyEmail_UsedToken_ReturnsFalse() {
        testToken.setUsed(true);
        when(tokenRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testToken));

        boolean result = emailService.verifyEmail("test-token-uuid");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Süresi dolmuş token ile doğrulama false dönmeli")
    void verifyEmail_ExpiredToken_ReturnsFalse() {
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testToken));

        boolean result = emailService.verifyEmail("test-token-uuid");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Keycloak hatası durumunda false dönmeli")
    void verifyEmail_KeycloakError_ReturnsFalse() {
        when(tokenRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testToken));
        when(userResource.toRepresentation()).thenThrow(new RuntimeException("Keycloak error"));

        boolean result = emailService.verifyEmail("test-token-uuid");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Geçerli token ile email başarıyla doğrulanmalı")
    void verifyEmail_ValidToken_ReturnsTrue() {
        when(tokenRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testToken));

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEmailVerified(false);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
        doNothing().when(userResource).update(any(UserRepresentation.class));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);

        boolean result = emailService.verifyEmail("test-token-uuid");

        assertThat(result).isTrue();
        assertThat(testToken.isUsed()).isTrue();
        verify(tokenRepository, times(1)).save(testToken);
    }
}
