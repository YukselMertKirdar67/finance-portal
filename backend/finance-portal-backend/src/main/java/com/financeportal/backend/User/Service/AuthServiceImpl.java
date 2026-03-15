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
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloakAdminClient;

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

