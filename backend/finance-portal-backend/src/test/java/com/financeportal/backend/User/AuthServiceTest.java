package com.financeportal.backend.User;

import com.financeportal.backend.Email.EmailService;
import com.financeportal.backend.Totp.TotpService;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.User.Service.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Unit Testleri")
class AuthServiceTest {

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TotpService totpService;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserRepresentation testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "realm", "finance-portal");

        testUser = new UserRepresentation();
        testUser.setId("test-user-id");
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setEnabled(true);
        testUser.setEmailVerified(false);

        when(keycloakAdminClient.realm("finance-portal")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(testUser);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
    }

    @Test
    @DisplayName("Şifre sıfırlama - email bulunamadığında başarılı mesaj dönmeli")
    void sendPasswordResetEmail_UserNotFound_ReturnsSuccess() {
        when(usersResource.search(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

        PasswordResetResponseDTO result = authService.sendPasswordResetEmail(
                new ForgotPasswordRequestDTO("notfound@test.com"));

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("If the email exists");
    }

    @Test
    @DisplayName("Şifre sıfırlama - email bulunduğunda verification email gönderilmeli")
    void sendPasswordResetEmail_UserFound_SendsEmail() throws Exception {
        when(usersResource.search(any(), any(), any(), eq("test@test.com"), anyInt(), anyInt()))
                .thenReturn(List.of(testUser));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        PasswordResetResponseDTO result = authService.sendPasswordResetEmail(
                new ForgotPasswordRequestDTO("test@test.com"));

        assertThat(result.isSuccess()).isTrue();
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Email doğrulama gönderme - kullanıcı bulunamadığında false dönmeli")
    void sendVerificationEmail_UserNotFound_ReturnsFalse() {
        when(usersResource.search(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

        EmailVerificationResponseDTO result = authService.sendVerificationEmail(
                new EmailVerificationRequestDTO("notfound@test.com"));

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Email doğrulama gönderme - email zaten doğrulanmışsa bilgi mesajı dönmeli")
    void sendVerificationEmail_AlreadyVerified_ReturnsInfo() {
        testUser.setEmailVerified(true);
        when(usersResource.search(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(testUser));

        EmailVerificationResponseDTO result = authService.sendVerificationEmail(
                new EmailVerificationRequestDTO("test@test.com"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(result.getMessage()).contains("already verified");
    }

    @Test
    @DisplayName("Email doğrulama kontrolü - doğrulanmış kullanıcı true dönmeli")
    void checkEmailVerification_Verified_ReturnsTrue() {
        testUser.setEmailVerified(true);
        when(usersResource.search(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(testUser));

        EmailVerificationResponseDTO result = authService.checkEmailVerification("test@test.com");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Email doğrulama kontrolü - kullanıcı bulunamadığında false dönmeli")
    void checkEmailVerification_UserNotFound_ReturnsFalse() {
        when(usersResource.search(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

        EmailVerificationResponseDTO result = authService.checkEmailVerification("notfound@test.com");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Şifre sıfırlama token işlemi başarıyla tamamlanmalı")
    void resetPassword_ReturnsSuccess() {
        PasswordResetResponseDTO result = authService.resetPassword(
                new ResetPasswordRequestDTO("test-token", "newPassword123"));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("2FA aktif olmayan kullanıcı için checkIfUserHasOTP false dönmeli")
    void checkIfUserHasOTP_NotEnabled_ReturnsFalse() {
        when(totpService.isTotpEnabled("test-user-id")).thenReturn(false);

        boolean result = authService.checkIfUserHasOTP("test-user-id");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("2FA aktif olan kullanıcı için checkIfUserHasOTP true dönmeli")
    void checkIfUserHasOTP_Enabled_ReturnsTrue() {
        when(totpService.isTotpEnabled("test-user-id")).thenReturn(true);

        boolean result = authService.checkIfUserHasOTP("test-user-id");

        assertThat(result).isTrue();
    }
}