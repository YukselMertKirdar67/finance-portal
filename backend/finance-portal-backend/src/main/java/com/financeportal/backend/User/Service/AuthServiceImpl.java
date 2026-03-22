package com.financeportal.backend.User.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloakAdminClient;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Override
    public RegisterResponseDTO registerUser(RegisterRequestDTO request) {
        log.info("Registering new user: {}", request.getUsername());

        try {
            // Keycloak realm resource al
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // User representation oluştur
            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(false); // Email verification için

            // Password credential oluştur
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            // User'ı Keycloak'ta oluştur
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                // Başarılı - User ID'yi al
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

                log.info("User registered successfully: {} with ID: {}", request.getUsername(), userId);

                // USER role'ünü ata
                assignUserRole(realmResource, userId);

                // Email verification gönder
                try {
                    usersResource.get(userId).executeActionsEmail(
                            Collections.singletonList("VERIFY_EMAIL")
                    );
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
                // User zaten var
                log.warn("User already exists: {}", request.getUsername());

                return RegisterResponseDTO.builder()
                        .success(false)
                        .message("Username or email already exists")
                        .build();

            } else {
                // Diğer hatalar
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

    @Override
    public PasswordResetResponseDTO sendPasswordResetEmail(ForgotPasswordRequestDTO request) {
        log.info("Sending password reset email to: {}", request.getEmail());

        try {
            // Keycloak realm resource al
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Email ile user'ı bul
            List<UserRepresentation> users = usersResource.search(null, null, null,
                    request.getEmail(), 0, 1);

            if (users.isEmpty()) {
                log.warn("User not found with email: {}", request.getEmail());

                // Güvenlik için "email sent" mesajı dön (email var mı yok mu belli etme)
                return PasswordResetResponseDTO.builder()
                        .success(true)
                        .message("If the email exists, a password reset link has been sent.")
                        .build();
            }

            UserRepresentation user = users.get(0);
            String userId = user.getId();

            // Keycloak'tan password reset email'i gönder
            usersResource.get(userId).executeActionsEmail(
                    Collections.singletonList("UPDATE_PASSWORD")
            );

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

    @Override
    public EmailVerificationResponseDTO sendVerificationEmail(EmailVerificationRequestDTO request) {
        log.info("Sending email verification to: {}", request.getEmail());

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(null, null, null,
                    request.getEmail(), 0, 1);

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

            // Email verification email'i gönder
            usersResource.get(userId).executeActionsEmail(
                    Collections.singletonList("VERIFY_EMAIL")
            );

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

    @Override
    public EmailVerificationResponseDTO checkEmailVerification(String email) {
        log.info("Checking email verification status for: {}", email);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Email ile user'ı bul
            List<UserRepresentation> users = usersResource.search(null, null, null,
                    email, 0, 1);

            if (users.isEmpty()) {
                log.warn("User not found with email: {}", email);

                return EmailVerificationResponseDTO.builder()
                        .success(false)
                        .message("User not found")
                        .emailVerified(false)
                        .build();
            }

            UserRepresentation user = users.get(0);
            boolean isVerified = Boolean.TRUE.equals(user.isEmailVerified());

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

            // OTP Code (Keycloak totp parametresi)
            if (request.getOtpCode() != null && !request.getOtpCode().isEmpty()) {
                body.add("totp", request.getOtpCode());
            }

            if (request.isRememberMe()) {
                body.add("scope", "openid offline_access");
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
                Map<String, Object> tokenResponse = response.getBody();

                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");

                // JWT decode
                Jwt jwt = jwtDecoder.decode(accessToken);
                String keycloakId = jwt.getSubject();
                String username = jwt.getClaimAsString("preferred_username");
                String email = jwt.getClaimAsString("email");

                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                List<String> roles = realmAccess != null
                        ? (List<String>) realmAccess.get("roles")
                        : Collections.emptyList();

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

                String errorBody = e.getResponseBodyAsString();
                log.debug("Error body: {}", errorBody);

                // Keycloak'tan gelen error: {"error":"invalid_grant","error_description":"Invalid user credentials"}

                // İlk denemede OTP yoksa, OTP_REQUIRED döndür
                if ((request.getOtpCode() == null || request.getOtpCode().isEmpty())) {

                    try {
                        boolean userHasOTP = checkIfUserHasOTPByUsername(request.getUsername());

                        if (userHasOTP) {
                            log.info("⚠️ User has OTP configured, requesting OTP");
                            return LoginResponseDTO.builder()
                                    .success(false)
                                    .message("OTP_REQUIRED")
                                    .build();
                        } else {
                            // OTP yok, username/password yanlış
                            return LoginResponseDTO.builder()
                                    .success(false)
                                    .message("Invalid username or password")
                                    .build();
                        }

                    } catch (Exception ex) {
                        log.error("Error checking OTP status: {}", ex.getMessage());
                        // Fallback: OTP_REQUIRED döndür (güvenli taraf)
                        return LoginResponseDTO.builder()
                                .success(false)
                                .message("OTP_REQUIRED")
                                .build();
                    }

                } else {
                    // OTP gönderilmiş ama hala 401 - OTP yanlış
                    log.warn("❌ Invalid OTP code for username: {}", request.getUsername());
                    return LoginResponseDTO.builder()
                            .success(false)
                            .message("Invalid OTP code. Please try again.")
                            .build();
                }
            }

        } catch (Exception e) {
            log.error("❌ Error during login: {}", e.getMessage(), e);

            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Login failed. Please try again.")
                    .build();
        }
    }

    @Override
    @Transactional
    public ChangePasswordResponseDTO changePassword(String userId, ChangePasswordRequestDTO request) {
        log.info("Changing password for user: {}", userId);

        try {
            // Şifrelerin eşleştiğini kontrol et
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password confirmation mismatch for user: {}", userId);

                return ChangePasswordResponseDTO.builder()
                        .success(false)
                        .message("Yeni şifre ve onay şifresi eşleşmiyor")
                        .build();
            }

            // Keycloak admin client
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // User'ı al
            UserRepresentation user = usersResource.get(userId).toRepresentation();
            String username = user.getUsername();

            // Mevcut şifreyi doğrula (OTP ile birlikte)
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

                // OTP varsa ekle
                if (request.getOtpCode() != null && !request.getOtpCode().isEmpty()) {
                    body.add("totp", request.getOtpCode());
                }

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

                try {
                    restTemplate.postForEntity(tokenUrl, entity, Map.class);
                    // Başarılı - şifre (ve OTP) doğru

                } catch (HttpClientErrorException e) {
                    // OTP gerekli mi kontrol et
                    boolean hasOTP = checkIfUserHasOTPByUsername(username);

                    if (hasOTP && (request.getOtpCode() == null || request.getOtpCode().isEmpty())) {
                        log.warn("⚠️ OTP required for user: {}", userId);

                        return ChangePasswordResponseDTO.builder()
                                .success(false)
                                .message("OTP_REQUIRED")
                                .build();
                    }

                    log.warn("❌ Current password or OTP is incorrect for user: {}", userId);

                    return ChangePasswordResponseDTO.builder()
                            .success(false)
                            .message("Mevcut şifre veya OTP kodu hatalı")
                            .build();
                }

            } catch (Exception e) {
                log.error("Error validating current password: {}", e.getMessage());

                return ChangePasswordResponseDTO.builder()
                        .success(false)
                        .message("Şifre doğrulama başarısız")
                        .build();
            }

            // Yeni şifreyi ayarla
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(false);

            usersResource.get(userId).resetPassword(credential);

            log.info("✅ Password changed successfully for user: {}", userId);

            // Son şifre değişim tarihini güncelle
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

    @Override
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        log.info("Refreshing access token");

        try {
            // Keycloak token endpoint
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

    @Override
    public PreAuthResponseDTO preAuth(LoginRequestDTO request) {
        log.info("Pre-auth check for username: {}", request.getUsername());

        try {
            // 1. Username + Password'ü Keycloak'a gönder (OTP olmadan)
            String tokenUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/token";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "finance-portal-frontend");
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());
            body.add("grant_type", "password");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            try {
                // Başarılı login (OTP yok)
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
                Map<String, Object> tokenResponse = response.getBody();

                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");

                Jwt jwt = jwtDecoder.decode(accessToken);
                String username = jwt.getClaimAsString("preferred_username");

                log.info("✅ Pre-auth successful (no OTP required) for: {}", username);

                return PreAuthResponseDTO.builder()
                        .success(true)
                        .message("Login successful")
                        .requiresOTP(false)
                        .build();

            } catch (HttpClientErrorException e) {
                // 401 - OTP gerekli veya yanlış credentials

                // User'ın OTP'si var mı kontrol et
                boolean userHasOTP = checkIfUserHasOTPByUsername(request.getUsername());

                if (userHasOTP) {
                    // OTP gerekli - Keycloak auth URL oluştur
                    String keycloakAuthUrl = buildKeycloakAuthUrl(request.getUsername());

                    log.info("⚠️ User has OTP, redirecting to Keycloak");

                    return PreAuthResponseDTO.builder()
                            .success(false)
                            .message("OTP_REQUIRED")
                            .requiresOTP(true)
                            .keycloakAuthUrl(keycloakAuthUrl)
                            .build();

                } else {
                    // OTP yok, username/password yanlış
                    log.warn("❌ Invalid credentials for: {}", request.getUsername());

                    return PreAuthResponseDTO.builder()
                            .success(false)
                            .message("Invalid username or password")
                            .requiresOTP(false)
                            .build();
                }
            }

        } catch (Exception e) {
            log.error("❌ Error during pre-auth: {}", e.getMessage(), e);

            return PreAuthResponseDTO.builder()
                    .success(false)
                    .message("Authentication failed")
                    .requiresOTP(false)
                    .build();
        }
    }

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

    @Override
    public LogoutResponseDTO logout(LogoutRequestDTO request) {
        log.info("Logout request - terminating Keycloak session");

        try {
            // Keycloak logout endpoint
            String logoutUrl = "http://finance-keycloak:8080/realms/finance-portal/protocol/openid-connect/logout";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "finance-portal-frontend");
            body.add("refresh_token", request.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            try {
                // Keycloak logout endpoint'ine istek at
                restTemplate.postForEntity(logoutUrl, entity, String.class);

                log.info("✅ Keycloak session terminated successfully");

                return LogoutResponseDTO.builder()
                        .success(true)
                        .message("Logout successful")
                        .build();

            } catch (HttpClientErrorException e) {
                log.warn("⚠️ Logout request failed (token may be already invalid): {}", e.getMessage());

                // Token zaten geçersiz olabilir, yine de başarılı say
                return LogoutResponseDTO.builder()
                        .success(true)
                        .message("Logout successful")
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Error during logout: {}", e.getMessage(), e);

            // Hata olsa bile frontend'de logout devam etsin
            return LogoutResponseDTO.builder()
                    .success(true)
                    .message("Logout completed (session cleanup may have failed)")
                    .build();
        }
    }



    private void assignUserRole(RealmResource realmResource, String userId) {
        try {
            // USER role'ünü bul
            var role = realmResource.roles().get("USER").toRepresentation();

            // User'a role ata
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(role));

            log.info("Assigned USER role to user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to assign USER role: {}", e.getMessage(), e);
        }
    }

    private String buildKeycloakAuthUrl(String username) {
        // Keycloak Authorization Code Flow URL
        String baseUrl = "http://localhost:8180/realms/finance-portal/protocol/openid-connect/auth";
        String clientId = "finance-portal-frontend";
        String redirectUri = "http://localhost:3000/auth/callback";
        String responseType = "code";
        String scope = "openid";

        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s&login_hint=%s",
                baseUrl, clientId, redirectUri, responseType, scope, username);
    }

    @Override
    public boolean checkIfUserHasOTP(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // User'ın credentials'larını kontrol et
            List<CredentialRepresentation> credentials = usersResource.get(userId).credentials();

            for (CredentialRepresentation cred : credentials) {
                if ("otp".equalsIgnoreCase(cred.getType())) {
                    return true;  // OTP var
                }
            }

            return false;  // OTP yok

        } catch (Exception e) {
            log.error("Error checking OTP for userId {}: {}", userId, e.getMessage());
            return false;
        }
    }
    private boolean checkIfUserHasOTPByUsername(String username) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Username ile user ara
            List<UserRepresentation> users = usersResource.search(username, true);

            if (users.isEmpty()) {
                return false;
            }

            String userId = users.get(0).getId();

            // Public metodu çağır
            return checkIfUserHasOTP(userId);

        } catch (Exception e) {
            log.error("Error checking OTP by username: {}", e.getMessage());
            return false;
        }
    }
}

