package com.financeportal.backend.User.Controller;

import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Service.AuthService;
import com.financeportal.backend.User.UserMapper;
import com.financeportal.backend.User.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Log4j2
@Tag(name = "Kullanıcı", description = "Giriş yapmış kullanıcının profil ve hesap yönetimi endpoint'leri")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthService authService;

    /**
     * Giriş yapmış kullanıcının temel profil bilgilerini getirir.
     */
    @Operation(summary = "Temel profil bilgilerini getir")
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
    @Operation(summary = "Detaylı profil bilgilerini getir", description = "Bildirim tercihleri ve tema bilgisi dahil profil döner")
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
    @Operation(summary = "Şifre değiştir", description = "2FA aktifse OTP kodu da istenir")
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
    @Operation(summary = "Kullanıcı adı güncelle")
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
    @Operation(summary = "E-posta adresi güncelle", description = "Güncelleme sonrası doğrulama e-postası gönderilir")
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
    @Operation(summary = "Son şifre değişim tarihini getir")
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
    @Operation(summary = "Tema ve bildirim tercihlerini güncelle")
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
    @Operation(summary = "Kullanıcı verilerini dışa aktar", description = "Tüm veriler JSON formatında indirilir")
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
    @Operation(summary = "Hesabı sil", description = "Tüm portföy, işlem ve watchlist verileri de kalıcı olarak silinir")
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
     * Kullanıcı kimlik doğrulamasını test eder.
     */
    @Operation(summary = "Kullanıcı kimlik doğrulama testi")
    @GetMapping("/ping")
    public String ping() {
        return "User authenticated";
    }


    /**
     * Admin yetkisini test eder.
     */
    @Operation(summary = "Admin yetki testi")
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
