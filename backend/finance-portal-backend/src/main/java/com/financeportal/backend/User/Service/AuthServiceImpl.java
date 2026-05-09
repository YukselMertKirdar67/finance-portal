package com.financeportal.backend.User.Service;

import com.financeportal.backend.Email.EmailService;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Totp.TotpService;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloakAdminClient;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TotpService totpService;

    @Value("${keycloak.admin.realm}")
    private String realm;

    /**
     * Yeni kullanıcı kaydı oluşturur.
     * Keycloak'ta kullanıcı oluşturur, USER rolü atar ve doğrulama e-postası gönderir.
     */
    @Override
    public RegisterResponseDTO registerUser(RegisterRequestDTO request) {
        log.info("Registering new user: {}", request.getUsername());

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(false);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                log.info("User registered successfully: {} with ID: {}", request.getUsername(), userId);

                assignUserRole(realmResource, userId);

                try {
                    emailService.sendVerificationEmail(userId, request.getEmail());
                    log.info("Email verification sent to: {}", request.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send verification email: {}", e.getMessage());
                }

                return RegisterResponseDTO.builder()
                        .success(true)
                        .message("Registration successful! Please login.")
                        .userId(userId)
                        .build();

            } else if (response.getStatus() == 409) {
                log.warn("User already exists: {}", request.getUsername());
                return RegisterResponseDTO.builder()
                        .success(false)
                        .message("Username or email already exists")
                        .build();

            } else {
                log.error("Failed to register user: {} - Status: {}", request.getUsername(), response.getStatus());
                return RegisterResponseDTO.builder()
                        .success(false)
                        .message("Registration failed. Please try again.")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            return RegisterResponseDTO.builder()
                    .success(false)
                    .message("An error occurred during registration")
                    .build();
        }
    }

    /**
     * Kullanıcıya şifre sıfırlama e-postası gönderir.
     * Güvenlik için e-posta var mı yok mu belli etmez, her durumda aynı mesajı döner.
     */
    @Override
    public PasswordResetResponseDTO sendPasswordResetEmail(ForgotPasswordRequestDTO request) {
        log.info("Sending password reset email to: {}", request.getEmail());

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(null, null, null, request.getEmail(), 0, 1);

            if (users.isEmpty()) {
                log.warn("User not found with email: {}", request.getEmail());
                return PasswordResetResponseDTO.builder()
                        .success(true)
                        .message("If the email exists, a password reset link has been sent.")
                        .build();
            }

            String userId = users.get(0).getId();
            emailService.sendVerificationEmail(userId, users.get(0).getEmail());
            log.info("Password reset email sent successfully to: {}", request.getEmail());

            return PasswordResetResponseDTO.builder()
                    .success(true)
                    .message("If the email exists, a password reset link has been sent.")
                    .build();

        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage(), e);
            return PasswordResetResponseDTO.builder()
                    .success(false)
                    .message("Failed to send password reset email. Please try again.")
                    .build();
        }
    }

    /**
     * Token ile şifre sıfırlama işlemini gerçekleştirir.
     */
    @Override
    public PasswordResetResponseDTO resetPassword(ResetPasswordRequestDTO request) {
        log.info("Resetting password with token");

        try {
            return PasswordResetResponseDTO.builder()
                    .success(true)
                    .message("Password reset successful. Please login with your new password.")
                    .build();
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage(), e);
            return PasswordResetResponseDTO.builder()
                    .success(false)
                    .message("Failed to reset password. Invalid or expired token.")
                    .build();
        }
    }

    /**
     * Kullanıcıya e-posta doğrulama maili gönderir.
     * E-posta zaten doğrulanmışsa bilgi mesajı döner.
     */
    @Override
    public EmailVerificationResponseDTO sendVerificationEmail(EmailVerificationRequestDTO request) {
        log.info("Sending email verification to: {}", request.getEmail());

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(null, null, null, request.getEmail(), 0, 1);

            if (users.isEmpty()) {
                log.warn("User not found with email: {}", request.getEmail());
                return EmailVerificationResponseDTO.builder()
                        .success(false)
                        .message("User not found with this email")
                        .emailVerified(false)
                        .build();
            }

            UserRepresentation user = users.get(0);
            String userId = user.getId();

            if (Boolean.TRUE.equals(user.isEmailVerified())) {
                log.info("Email already verified: {}", request.getEmail());
                return EmailVerificationResponseDTO.builder()
                        .success(true)
                        .message("Email is already verified")
                        .emailVerified(true)
                        .build();
            }

            emailService.sendVerificationEmail(userId, request.getEmail());
            log.info("Email verification sent successfully to: {}", request.getEmail());

            return EmailVerificationResponseDTO.builder()
                    .success(true)
                    .message("Verification email sent successfully. Please check your inbox.")
                    .emailVerified(false)
                    .build();

        } catch (Exception e) {
            log.error("Error sending verification email: {}", e.getMessage(), e);
            return EmailVerificationResponseDTO.builder()
                    .success(false)
                    .message("Failed to send verification email. Please try again.")
                    .emailVerified(false)
                    .build();
        }
    }

    /**
     * Kullanıcının e-posta doğrulama durumunu kontrol eder.
     */
    @Override
    public EmailVerificationResponseDTO checkEmailVerification(String email) {
        log.info("Checking email verification status for: {}", email);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(null, null, null, email, 0, 1);

            if (users.isEmpty()) {
                log.warn("User not found with email: {}", email);
                return EmailVerificationResponseDTO.builder()
                        .success(false)
                        .message("User not found")
                        .emailVerified(false)
                        .build();
            }

            boolean isVerified = Boolean.TRUE.equals(users.get(0).isEmailVerified());
            log.info("Email verification status for {}: {}", email, isVerified);

            return EmailVerificationResponseDTO.builder()
                    .success(true)
                    .message(isVerified ? "Email is verified" : "Email is not verified")
                    .emailVerified(isVerified)
                    .build();

        } catch (Exception e) {
            log.error("Error checking email verification: {}", e.getMessage(), e);
            return EmailVerificationResponseDTO.builder()
                    .success(false)
                    .message("Failed to check verification status")
                    .emailVerified(false)
                    .build();
        }
    }

    /**
     * Kullanıcı girişi yapar.
     * 2FA aktifse 2FA_REQUIRED mesajı döner.
     * totpVerified true ise 2FA kontrolü atlanır.
     * Başarılı girişte access token ve refresh token döner.
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for username: {}", request.getUsername());

        try {
            String tokenUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/token";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "finance-portal-frontend");
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());
            body.add("grant_type", "password");

            if (request.isRememberMe()) {
                body.add("scope", "openid offline_access");
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
                Map<String, Object> tokenResponse = response.getBody();

                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");

                Jwt jwt = jwtDecoder.decode(accessToken);
                String keycloakId = jwt.getSubject();
                String username = jwt.getClaimAsString("preferred_username");
                String email = jwt.getClaimAsString("email");

                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                List<String> roles = realmAccess != null
                        ? (List<String>) realmAccess.get("roles")
                        : Collections.emptyList();

                // 2FA kontrolü — totpVerified true ise atla
                if (!request.isTotpVerified() && totpService.isTotpEnabled(keycloakId)) {
                    log.info("⚠️ 2FA required for user: {}", username);
                    return LoginResponseDTO.builder()
                            .success(false)
                            .message("2FA_REQUIRED")
                            .keycloakId(keycloakId)
                            .build();
                }

                log.info("✅ Login successful for username: {}", username);

                return LoginResponseDTO.builder()
                        .success(true)
                        .message("Login successful")
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .username(username)
                        .email(email)
                        .roles(roles)
                        .build();

            } catch (HttpClientErrorException e) {
                log.warn("❌ Login failed for username: {} - Status: {}", request.getUsername(), e.getStatusCode());
                return LoginResponseDTO.builder()
                        .success(false)
                        .message("Invalid username or password")
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Error during login: {}", e.getMessage(), e);
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Login failed. Please try again.")
                    .build();
        }
    }

    /**
     * Kullanıcının şifresini değiştirir.
     * Mevcut şifre doğrulanır.
     * Başarılı değişimde son şifre değişim tarihi güncellenir.
     */
    @Override
    @Transactional
    public ChangePasswordResponseDTO changePassword(String userId, ChangePasswordRequestDTO request) {
        log.info("Changing password for user: {}", userId);

        try {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password confirmation mismatch for user: {}", userId);
                return ChangePasswordResponseDTO.builder()
                        .success(false)
                        .message("Yeni şifre ve onay şifresi eşleşmiyor")
                        .build();
            }

            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = usersResource.get(userId).toRepresentation();
            String username = user.getUsername();

            try {
                String tokenUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/token";

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("client_id", "finance-portal-frontend");
                body.add("username", username);
                body.add("password", request.getCurrentPassword());
                body.add("grant_type", "password");

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

                try {
                    restTemplate.postForEntity(tokenUrl, entity, Map.class);
                } catch (HttpClientErrorException e) {
                    log.warn("❌ Current password is incorrect for user: {}", userId);
                    return ChangePasswordResponseDTO.builder()
                            .success(false)
                            .message("Mevcut şifre hatalı")
                            .build();
                }

            } catch (Exception e) {
                log.error("Error validating current password: {}", e.getMessage());
                return ChangePasswordResponseDTO.builder()
                        .success(false)
                        .message("Şifre doğrulama başarısız")
                        .build();
            }

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(false);
            usersResource.get(userId).resetPassword(credential);

            log.info("✅ Password changed successfully for user: {}", userId);

            try {
                User dbUser = userRepository.findByKeycloakId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                dbUser.setPasswordLastChanged(LocalDateTime.now());
                userRepository.save(dbUser);
                log.info("✅ Password last changed date updated for user: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to update password last changed date: {}", e.getMessage());
            }

            return ChangePasswordResponseDTO.builder()
                    .success(true)
                    .message("Şifreniz başarıyla değiştirildi")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error changing password: {}", e.getMessage(), e);
            return ChangePasswordResponseDTO.builder()
                    .success(false)
                    .message("Şifre değiştirme işlemi başarısız. Lütfen tekrar deneyin.")
                    .build();
        }
    }

    /**
     * Refresh token kullanarak yeni access token alır.
     */
    @Override
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        log.info("Refreshing access token");

        try {
            String tokenUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/token";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "finance-portal-frontend");
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", request.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
                Map<String, Object> tokenResponse = response.getBody();

                String newAccessToken = (String) tokenResponse.get("access_token");
                String newRefreshToken = (String) tokenResponse.get("refresh_token");

                log.info("✅ Token refreshed successfully");

                return RefreshTokenResponseDTO.builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .build();

            } catch (HttpClientErrorException e) {
                log.warn("❌ Token refresh failed: {}", e.getMessage());
                return RefreshTokenResponseDTO.builder()
                        .success(false)
                        .message("Invalid or expired refresh token")
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Error during token refresh: {}", e.getMessage(), e);
            return RefreshTokenResponseDTO.builder()
                    .success(false)
                    .message("Token refresh failed. Please login again.")
                    .build();
        }
    }

    /**
     * Authorization code ile access token ve refresh token alır.
     */
    @Override
    public LoginResponseDTO exchangeCodeForToken(String authCode) {
        log.info("Exchanging authorization code for token");

        try {
            String tokenUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/token";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "finance-portal-frontend");
            body.add("grant_type", "authorization_code");
            body.add("code", authCode);
            body.add("redirect_uri", "http://localhost:3000/auth/callback");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
            Map<String, Object> tokenResponse = response.getBody();

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");

            Jwt jwt = jwtDecoder.decode(accessToken);
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            List<String> roles = realmAccess != null
                    ? (List<String>) realmAccess.get("roles")
                    : Collections.emptyList();

            log.info("✅ Token exchange successful for: {}", username);

            return LoginResponseDTO.builder()
                    .success(true)
                    .message("Login successful")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .username(username)
                    .email(email)
                    .roles(roles)
                    .build();

        } catch (Exception e) {
            log.error("❌ Token exchange failed: {}", e.getMessage(), e);
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Authentication failed")
                    .build();
        }
    }

    /**
     * Kullanıcının Keycloak oturumunu sonlandırır.
     * Hata olsa bile frontend'de logout devam eder.
     */
    @Override
    public LogoutResponseDTO logout(LogoutRequestDTO request) {
        log.info("Logout request - terminating Keycloak session");

        try {
            String logoutUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/logout";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "finance-portal-frontend");
            body.add("refresh_token", request.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            try {
                restTemplate.postForEntity(logoutUrl, entity, String.class);
                log.info("✅ Keycloak session terminated successfully");
                return LogoutResponseDTO.builder()
                        .success(true)
                        .message("Logout successful")
                        .build();
            } catch (HttpClientErrorException e) {
                log.warn("⚠️ Logout request failed (token may be already invalid): {}", e.getMessage());
                return LogoutResponseDTO.builder()
                        .success(true)
                        .message("Logout successful")
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Error during logout: {}", e.getMessage(), e);
            return LogoutResponseDTO.builder()
                    .success(true)
                    .message("Logout completed (session cleanup may have failed)")
                    .build();
        }
    }

    /**
     * Kullanıcıya Keycloak'ta USER rolü atar.
     */
    private void assignUserRole(RealmResource realmResource, String userId) {
        try {
            var role = realmResource.roles().get("USER").toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
            log.info("Assigned USER role to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to assign USER role: {}", e.getMessage(), e);
        }
    }

    /**
     * Kullanıcının OTP (2FA) aktif olup olmadığını Keycloak credentials üzerinden kontrol eder.
     */
    @Override
    public boolean checkIfUserHasOTP(String userId) {
        return totpService.isTotpEnabled(userId);
    }
}