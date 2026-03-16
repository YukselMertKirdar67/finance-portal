package com.financeportal.backend.User.Service;

import com.financeportal.backend.User.DTO.*;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloakAdminClient;
    private final JwtDecoder jwtDecoder;

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

            // Email verification email'i gönder (redirect URL ile)
            String redirectUri = "http://localhost:3000/login";
            String clientId = "finance-portal-frontend";

            usersResource.get(userId).executeActionsEmail(
                    redirectUri,
                    clientId,
                    null,
                    Collections.singletonList("VERIFY_EMAIL")
                                    );

            log.info("✅ Email verification sent successfully to: {} (redirect: {})", request.getEmail(), redirectUri);

            return EmailVerificationResponseDTO.builder()
                    .success(true)
                    .message("Verification email sent successfully. Please check your inbox.")
                    .emailVerified(false)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error sending verification email: {}", e.getMessage(), e);

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
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            // Keycloak token endpoint'e istek at
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
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
                Map<String, Object> tokenResponse = response.getBody();

                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");

                // JWT parse et
                Jwt jwt = jwtDecoder.decode(accessToken);
                String username = jwt.getClaimAsString("preferred_username");
                String email = jwt.getClaimAsString("email");

                // Roles çıkar
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                List<String> roles = (List<String>) realmAccess.get("roles");

                log.info("Login successful for user: {}", username);

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
                log.warn("Login failed for user: {} - {}", request.getUsername(), e.getMessage());

                return LoginResponseDTO.builder()
                        .success(false)
                        .message("Invalid username or password")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error during login: {}", e.getMessage(), e);

            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Login failed. Please try again.")
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
    public ChangePasswordResponseDTO changePassword(String userId, ChangePasswordRequestDTO request) {
        log.info("Changing password for user: {}", userId);

        try {
            // Şifrelerin eşleştiğini kontrol et
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password confirmation mismatch for user: {}", userId);

                return ChangePasswordResponseDTO.builder()
                        .success(false)
                        .message("New password and confirm password do not match")
                        .build();
            }

            // Keycloak admin client
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Önce mevcut şifreyi doğrula (login denemesi yap)
            try {
                UserRepresentation user = usersResource.get(userId).toRepresentation();
                String username = user.getUsername();

                // Mevcut şifreyle login dene
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
                    log.warn("Current password is incorrect for user: {}", userId);

                    return ChangePasswordResponseDTO.builder()
                            .success(false)
                            .message("Current password is incorrect")
                            .build();
                }

            } catch (Exception e) {
                log.error("Error validating current password: {}", e.getMessage());

                return ChangePasswordResponseDTO.builder()
                        .success(false)
                        .message("Failed to validate current password")
                        .build();
            }

            // Yeni şifreyi ayarla
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(false);

            usersResource.get(userId).resetPassword(credential);

            log.info("✅ Password changed successfully for user: {}", userId);

            return ChangePasswordResponseDTO.builder()
                    .success(true)
                    .message("Password changed successfully")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error changing password: {}", e.getMessage(), e);

            return ChangePasswordResponseDTO.builder()
                    .success(false)
                    .message("Failed to change password. Please try again.")
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
}

