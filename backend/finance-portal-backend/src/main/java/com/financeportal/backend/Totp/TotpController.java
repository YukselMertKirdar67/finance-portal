package com.financeportal.backend.Totp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/totp")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "2FA (TOTP)", description = "İki faktörlü doğrulama yönetimi endpoint'leri")

public class TotpController {

    private final TotpService totpService;

    /**
     * TOTP kurulumu başlatır, QR kod ve secret döner.
     */
    @Operation(summary = "TOTP kurulumu başlat", description = "QR kod ve secret key döner")
    @PostMapping("/setup")
    public ResponseEntity<?> setupTotp(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        log.info("TOTP setup request for user: {}", keycloakId);
        Map<String, String> result = totpService.setupTotp(keycloakId, email);
        return ResponseEntity.ok(result);
    }

    /**
     * TOTP kurulumunu doğrular ve aktif eder.
     */
    @Operation(summary = "TOTP kurulumunu doğrula", description = "Kodu doğrular ve 2FA'yı aktif eder")
    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifySetup(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {
        String keycloakId = jwt.getSubject();
        String code = body.get("code");
        log.info("TOTP setup verification for user: {}", keycloakId);
        boolean success = totpService.verifyAndActivateTotp(keycloakId, code);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "2FA başarıyla aktif edildi"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Geçersiz kod"));
        }
    }

    /**
     * Login sırasında TOTP kodunu doğrular.
     */
    @Operation(summary = "Giriş sırasında TOTP kodunu doğrula")
    @PostMapping("/verify-login")
    public ResponseEntity<?> verifyLogin(@RequestBody Map<String, String> body) {
        String keycloakId = body.get("keycloakId");
        String code = body.get("code");
        log.info("TOTP login verification for user: {}", keycloakId);
        boolean success = totpService.verifyTotpCode(keycloakId, code);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Kod doğrulandı"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Geçersiz kod"));
        }
    }

    /**
     * Kullanıcının 2FA durumunu kontrol eder.
     */
    @Operation(summary = "2FA durumunu kontrol et", description = "Kullanıcının 2FA aktif olup olmadığını döner")
    @GetMapping("/status")
    public ResponseEntity<?> getTotpStatus(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        boolean enabled = totpService.isTotpEnabled(keycloakId);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    /**
     * 2FA'yı devre dışı bırakır.
     */
    @Operation(summary = "2FA'yı devre dışı bırak")
    @DeleteMapping("/disable")
    public ResponseEntity<?> disableTotp(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        log.info("TOTP disable request for user: {}", keycloakId);
        totpService.disableTotp(keycloakId);
        return ResponseEntity.ok(Map.of("success", true, "message", "2FA devre dışı bırakıldı"));
    }
}
