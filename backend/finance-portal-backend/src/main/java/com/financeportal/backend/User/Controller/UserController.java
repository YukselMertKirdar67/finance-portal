package com.financeportal.backend.User.Controller;

import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Service.AuthService;
import com.financeportal.backend.User.UserMapper;
import com.financeportal.backend.User.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthService authService;

    /**
     * Giriş yapmış kullanıcının temel profil bilgilerini getirir.
     */
    @GetMapping
    public MeResponseDTO me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        List<String> roles = extractRoles(jwt);
        return userMapper.toMeResponseDTO(user, roles);
    }

    /**
     * Giriş yapmış kullanıcının detaylı profil bilgilerini getirir.
     * Bildirim tercihleri ve tema bilgisi de dahildir.
     */
    @GetMapping("/profile")
    public UserProfileDTO getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        log.info("Fetching current user profile");

        String keycloakId = jwt.getSubject();

        // Database'den al (güncel username ve email için)
        User user = userService.getOrCreateUser(jwt);

        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = realmAccess != null
                ? (List<String>) realmAccess.get("roles")
                : List.of();

        return UserProfileDTO.builder()
                .id(keycloakId)
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(emailVerified != null ? emailVerified : false)
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .theme(user.getTheme())
                .notifyTransaction(user.isNotifyTransaction())
                .notifyPortfolioChange(user.isNotifyPortfolioChange())
                .notifyPriceAlert(user.isNotifyPriceAlert())
                .notifyNews(user.isNotifyNews())
                .build();
    }

    /**
     * Giriş yapmış kullanıcının şifresini değiştirir.
     * OTP aktifse doğrulama kodu da istenir.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponseDTO> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequestDTO request) {

        log.info("Password change request received");

        String userId = jwt.getSubject();

        ChangePasswordResponseDTO response = authService.changePassword(userId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Giriş yapmış kullanıcının kullanıcı adını günceller.
     */

    @PutMapping("/username")
    public ResponseEntity<?> updateUsername(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUsernameRequestDTO request) {

        try {
            String userId = jwt.getSubject();
            userService.updateUsername(userId, request.getNewUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kullanıcı adınız başarıyla güncellendi"
            ));

        } catch (RuntimeException e) {
            log.error("Username update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Giriş yapmış kullanıcının e-posta adresini günceller.
     * Doğrulama e-postası gönderilir.
     */

    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateEmailRequestDTO request) {

        try {
            String userId = jwt.getSubject();
            userService.updateEmail(userId, request.getNewEmail(), request.getPassword());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Doğrulama e-postası gönderildi. Lütfen e-postanızı kontrol edin."
            ));

        } catch (RuntimeException e) {
            log.error("Email update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Kullanıcının son şifre değişim tarihini getirir.
     */

    @GetMapping("/password-last-changed")
    public ResponseEntity<PasswordLastChangedDTO> getPasswordLastChanged(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        LocalDateTime lastChanged = userService.getPasswordLastChanged(userId);

        return ResponseEntity.ok(PasswordLastChangedDTO.builder()
                .lastChanged(lastChanged)
                .build());
    }

    /**
     * Kullanıcının tema ve bildirim tercihlerini günceller.
     */
    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePreferencesRequestDTO request) {

        try {
            User user = userService.getOrCreateUser(jwt);
            String userId = user.getKeycloakId();

            userService.updatePreferences(
                    userId,
                    request.getTheme(),
                    request.getNotifyTransaction(),
                    request.getNotifyPortfolioChange(),
                    request.getNotifyPriceAlert(),
                    request.getNotifyNews()
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Tercihleriniz başarıyla güncellendi"));

        } catch (RuntimeException e) {
            log.error("Preferences update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Kullanıcının tüm verilerini JSON formatında dışa aktarır.
     */

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUserData(@AuthenticationPrincipal Jwt jwt) {
        try {
            User user = userService.getOrCreateUser(jwt);
            byte[] data = userService.exportUserData(user.getKeycloakId());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=finance-portal-data.json")
                    .header("Content-Type", "application/json")
                    .body(data);
        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Kullanıcının hesabını kalıcı olarak siler.
     * Tüm portföy, işlem ve watchlist verileri de silinir.
     */

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        try {
            User user = userService.getOrCreateUser(jwt);
            userService.deleteAccount(user.getKeycloakId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Hesabınız başarıyla silindi"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Kullanıcının OTP (2FA) aktif olup olmadığını kontrol eder.
     */

    @GetMapping("/has-otp")
    public ResponseEntity<Map<String, Boolean>> checkIfUserHasOTP(@AuthenticationPrincipal Jwt jwt) {
        log.info("Checking if user has OTP enabled");

        String userId = jwt.getSubject();
        boolean hasOTP = authService.checkIfUserHasOTP(userId);

        log.info("User {} has OTP: {}", userId, hasOTP);

        return ResponseEntity.ok(Map.of("hasOTP", hasOTP));
    }

    /**
     * Kullanıcı kimlik doğrulamasını test eder.
     */

    @GetMapping("/ping")
    public String ping() {
        return "User authenticated";
    }


    /**
     * Admin yetkisini test eder.
     */

    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCheck() {
        return "Admin access granted";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return Collections.emptyList();
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null ? roles : Collections.emptyList();
    }
}
