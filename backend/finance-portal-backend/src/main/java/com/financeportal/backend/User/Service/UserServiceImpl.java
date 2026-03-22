package com.financeportal.backend.User.Service;


import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public User getOrCreateUser(Jwt jwt) {
        String keycloakId = jwt.getSubject(); // sub
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    User user = User.builder()
                            .keycloakId(keycloakId)
                            .username(username)
                            .email(email)
                            .enabled(true)
                            .build();
                    return userRepository.save(user);
                });
    }

    @Override
    public User getByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with keycloakId: " + keycloakId));
    }

    /**
     * Kullanıcı adını güncelle
     */
    @Override
    @Transactional
    public void updateUsername(String userId, String newUsername) {
        log.info("Updating username for user: {}", userId);

        // Kullanıcıyı bul
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Username unique kontrolü
        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor");
        }

        // Keycloak'ta güncelle
        try {
            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(userId);

            UserRepresentation keycloakUser = userResource.toRepresentation();
            keycloakUser.setUsername(newUsername);
            userResource.update(keycloakUser);

            log.info("Username updated in Keycloak: {}", newUsername);

        } catch (Exception e) {
            log.error("Failed to update username in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Keycloak güncellemesi başarısız: " + e.getMessage());
        }

        // Local DB'de güncelle
        user.setUsername(newUsername);
        userRepository.save(user);

        log.info("✅ Username updated successfully: {}", newUsername);
    }

    /**
     * E-posta adresini güncelle
     */
    @Override
    @Transactional
    public void updateEmail(String userId, String newEmail, String password) {
        log.info("Updating email for user: {}", userId);

        // Kullanıcıyı bul
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Email unique kontrolü
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
        }

        // Şifre doğrulama (Keycloak token endpoint ile)
        try {
            validatePassword(user.getUsername(), password);
        } catch (Exception e) {
            throw new RuntimeException("Şifre yanlış");
        }

        // Keycloak'ta güncelle
        try {
            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(userId);

            UserRepresentation keycloakUser = userResource.toRepresentation();
            keycloakUser.setEmail(newEmail);
            keycloakUser.setEmailVerified(false);  // ⚠️ Doğrulama gerekli
            userResource.update(keycloakUser);

            // Doğrulama e-postası gönder
            userResource.executeActionsEmail(List.of("VERIFY_EMAIL"));

            log.info("Email updated in Keycloak, verification email sent: {}", newEmail);

        } catch (Exception e) {
            log.error("Failed to update email in Keycloak: {}", e.getMessage());
            throw new RuntimeException("E-posta güncellemesi başarısız: " + e.getMessage());
        }

        // Local DB'de güncelle (verified false olarak)
        user.setEmail(newEmail);
        userRepository.save(user);

        log.info("✅ Email update initiated, verification required: {}", newEmail);
    }

    /**
     * Şifre doğrulama helper
     */
    private void validatePassword(String username, String password) {
        RestTemplate restTemplate = new RestTemplate();

        String tokenUrl = "http://finance-keycloak:8080/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=password&client_id=finance-portal-frontend&username=" +
                username + "&password=" + password;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(tokenUrl, request, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid password");
        }
    }

    /**
     * Son şifre değişim tarihini getir
     */
    @Override
    public LocalDateTime getPasswordLastChanged(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return user.getPasswordLastChanged();
    }
}

