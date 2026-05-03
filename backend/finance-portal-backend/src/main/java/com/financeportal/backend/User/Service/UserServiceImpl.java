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

    /**
     * JWT token'dan kullanıcı bilgilerini alır.
     * Kullanıcı yoksa yeni oluşturur, varsa mevcut kullanıcıyı döner.
     */
    @Override
    public User getOrCreateUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    log.info("Creating new user: {}", username);
                    User user = User.builder()
                            .keycloakId(keycloakId)
                            .username(username)
                            .email(email)
                            .enabled(true)
                            .build();
                    User saved = userRepository.save(user);
                    log.info("✅ New user created: {}", username);
                    return saved;
                });
    }

    /**
     * Keycloak ID'sine göre kullanıcıyı getirir.
     * Kullanıcı bulunamazsa RuntimeException fırlatır.
     */
    @Override
    public User getByKeycloakId(String keycloakId) {
        log.info("Fetching user by keycloakId: {}", keycloakId);
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    log.error("User not found with keycloakId: {}", keycloakId);
                    return new RuntimeException("User not found with keycloakId: " + keycloakId);
                });
    }

    /**
     * Kullanıcı adını hem Keycloak'ta hem de local DB'de günceller.
     * Kullanıcı adı benzersiz olmalıdır.
     */
    @Override
    @Transactional
    public void updateUsername(String userId, String newUsername) {
        log.info("Updating username for user: {}", userId);

        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByUsername(newUsername)) {
            log.warn("Username already exists: {}", newUsername);
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor");
        }

        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation keycloakUser = userResource.toRepresentation();
            keycloakUser.setUsername(newUsername);
            userResource.update(keycloakUser);
            log.info("Username updated in Keycloak: {}", newUsername);
        } catch (Exception e) {
            log.error("Failed to update username in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Keycloak güncellemesi başarısız: " + e.getMessage());
        }

        user.setUsername(newUsername);
        userRepository.save(user);
        log.info("✅ Username updated successfully: {}", newUsername);
    }

    /**
     * E-posta adresini hem Keycloak'ta hem de local DB'de günceller.
     * Şifre doğrulaması yapılır ve doğrulama e-postası gönderilir.
     */
    @Override
    @Transactional
    public void updateEmail(String userId, String newEmail, String password) {
        log.info("Updating email for user: {}", userId);

        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmail(newEmail)) {
            log.warn("Email already exists: {}", newEmail);
            throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
        }

        try {
            validatePassword(user.getUsername(), password);
        } catch (Exception e) {
            log.warn("Password validation failed for user: {}", userId);
            throw new RuntimeException("Şifre yanlış");
        }

        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation keycloakUser = userResource.toRepresentation();
            keycloakUser.setEmail(newEmail);
            keycloakUser.setEmailVerified(false);
            userResource.update(keycloakUser);
            userResource.executeActionsEmail(List.of("VERIFY_EMAIL"));
            log.info("Email updated in Keycloak, verification email sent: {}", newEmail);
        } catch (Exception e) {
            log.error("Failed to update email in Keycloak: {}", e.getMessage());
            throw new RuntimeException("E-posta güncellemesi başarısız: " + e.getMessage());
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        log.info("✅ Email update initiated, verification required: {}", newEmail);
    }

    /**
     * Keycloak token endpoint üzerinden şifre doğrulaması yapar.
     */
    private void validatePassword(String username, String password) {
        log.debug("Validating password for username: {}", username);
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
            log.warn("Password validation failed for username: {}", username);
            throw new RuntimeException("Invalid password");
        }
    }

    /**
     * Kullanıcının son şifre değişim tarihini getirir.
     */
    @Override
    public LocalDateTime getPasswordLastChanged(String userId) {
        log.info("Fetching password last changed for user: {}", userId);
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getPasswordLastChanged();
    }

    /**
     * Kullanıcının tema ve bildirim tercihlerini günceller.
     */
    @Override
    @Transactional
    public void updatePreferences(String userId, String theme, Boolean notifyTransaction,
                                  Boolean notifyPortfolioChange, Boolean notifyPriceAlert,
                                  Boolean notifyNews) {
        log.info("Updating preferences for user: {}", userId);

        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (theme != null && !theme.isEmpty()) {
            user.setTheme(theme);
            log.info("Theme updated to: {}", theme);
        }
        if (notifyTransaction != null) user.setNotifyTransaction(notifyTransaction);
        if (notifyPortfolioChange != null) user.setNotifyPortfolioChange(notifyPortfolioChange);
        if (notifyPriceAlert != null) user.setNotifyPriceAlert(notifyPriceAlert);
        if (notifyNews != null) user.setNotifyNews(notifyNews);

        userRepository.save(user);
        log.info("✅ Preferences updated successfully for user: {}", userId);
    }

    /**
     * Kullanıcının tüm portföy ve işlem verilerini JSON formatında dışa aktarır.
     */
    @Override
    public byte[] exportUserData(String userId) throws Exception {
        log.info("Exporting data for user: {}", userId);

        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        log.info("Found {} portfolios for user: {}", portfolios.size(), userId);

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

        log.info("✅ Data export completed for user: {}", userId);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exportData);
    }

    /**
     * Kullanıcının tüm verilerini (portföy, işlem, watchlist) siler
     * ve Keycloak'tan hesabı kaldırır.
     */
    @Override
    @Transactional
    public void deleteAccount(String userId) {
        log.info("Deleting account for user: {}", userId);

        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        log.info("Deleting {} portfolios for user: {}", portfolios.size(), userId);

        for (Portfolio portfolio : portfolios) {
            transactionRepository.deleteAllByPortfolioId(portfolio.getId());
            holdingRepository.deleteAllByPortfolioId(portfolio.getId());
            portfolioRepository.delete(portfolio);
        }

        watchlistRepository.deleteAllByUserId(userId);
        log.info("Watchlist deleted for user: {}", userId);

        try {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("✅ User deleted from Keycloak: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to delete user from Keycloak: {}", e.getMessage());
            throw new RuntimeException("Keycloak'tan kullanıcı silinemedi: " + e.getMessage());
        }

        userRepository.delete(user);
        log.info("✅ Account deleted successfully for user: {}", userId);
    }
}