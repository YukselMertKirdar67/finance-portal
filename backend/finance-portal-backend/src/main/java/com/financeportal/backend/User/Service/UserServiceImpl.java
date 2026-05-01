package com.financeportal.backend.User.Service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.Watchlist.WatchlistRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final PortfolioRepository portfolioRepository;
    private final WatchlistRepository watchlistRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioHoldingRepository holdingRepository;

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
            keycloakUser.setEmailVerified(false);
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

    /**
     * Kullanıcı tercihlerini güncelle
     */
    @Override
    @Transactional
    public void updatePreferences(String userId, String theme, Boolean notifyTransaction,
                                  Boolean notifyPortfolioChange, Boolean notifyPriceAlert,
                                  Boolean notifyNews) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (theme != null && !theme.isEmpty()) user.setTheme(theme);
        if (notifyTransaction != null) user.setNotifyTransaction(notifyTransaction);
        if (notifyPortfolioChange != null) user.setNotifyPortfolioChange(notifyPortfolioChange);
        if (notifyPriceAlert != null) user.setNotifyPriceAlert(notifyPriceAlert);
        if (notifyNews != null) user.setNotifyNews(notifyNews);

        userRepository.save(user);
        log.info("✅ Preferences updated successfully");
    }

    @Override
    public byte[] exportUserData(String userId) throws Exception {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Portföyleri çek
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

        // JSON oluştur
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("username", user.getUsername());
        exportData.put("email", user.getEmail());
        exportData.put("exportDate", LocalDateTime.now().toString());
        exportData.put("portfolios", portfolios.stream().map(p -> {
            Map<String, Object> portfolioMap = new HashMap<>();
            portfolioMap.put("name", p.getName());
            portfolioMap.put("currency", p.getCurrency());
            portfolioMap.put("type", p.getPortfolioType());
            portfolioMap.put("createdAt", p.getCreatedAt().toString());

            // İşlemleri ekle
            List<PortfolioTransaction> transactions = transactionRepository
                    .findByPortfolioIdAndDeletedFalseOrderByTransactionDateDesc(p.getId());

            portfolioMap.put("transactions", transactions.stream().map(t -> {
                Map<String, Object> txMap = new HashMap<>();
                txMap.put("instrument", t.getInstrument().getSymbol());
                txMap.put("type", t.getTransactionType());
                txMap.put("quantity", t.getQuantity());
                txMap.put("price", t.getPrice());
                txMap.put("currency", t.getCurrency());
                txMap.put("date", t.getTransactionDate().toString());
                return txMap;
            }).collect(Collectors.toList()));

            return portfolioMap;
        }).collect(Collectors.toList()));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exportData);
    }

    @Override
    @Transactional
    public void deleteAccount(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Portföyleri ve bağlı verileri sil
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        for (Portfolio portfolio : portfolios) {
            transactionRepository.deleteAllByPortfolioId(portfolio.getId());
            holdingRepository.deleteAllByPortfolioId(portfolio.getId());
            portfolioRepository.delete(portfolio);
        }

        // Watchlist sil
        watchlistRepository.deleteAllByUserId(userId);

        // Keycloak'tan sil
        try {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("✅ User deleted from Keycloak: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to delete user from Keycloak: {}", e.getMessage());
            throw new RuntimeException("Keycloak'tan kullanıcı silinemedi: " + e.getMessage());
        }

        // Local DB'den sil
        userRepository.delete(user);

        log.info("✅ Account deleted for user: {}", userId);
    }
}

