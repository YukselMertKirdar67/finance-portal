package com.financeportal.backend.User.Controller;

import com.financeportal.backend.Email.EmailService;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Service.AuthService;
import com.financeportal.backend.User.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Auth", description = "Kimlik doğrulama ve kullanıcı kayıt endpoint'leri")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtDecoder jwtDecoder;
    private final EmailService emailService;

    /**
     * Yeni kullanıcı kaydı oluşturur.
     */
    @Operation(summary = "Kullanıcı kaydı", description = "Yeni kullanıcı oluşturur ve doğrulama e-postası gönderir")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO response = authService.registerUser(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kullanıcıya e-posta doğrulama maili gönderir.
     */
    @Operation(summary = "E-posta doğrulama maili gönder")
    @PostMapping("/send-verification-email")
    public ResponseEntity<EmailVerificationResponseDTO> sendVerificationEmail(
            @Valid @RequestBody EmailVerificationRequestDTO request) {

        EmailVerificationResponseDTO response = authService.sendVerificationEmail(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kullanıcının e-posta doğrulama durumunu kontrol eder.
     */
    @Operation(summary = "E-posta doğrulama durumunu kontrol et")
    @GetMapping("/check-email-verification")
    public ResponseEntity<EmailVerificationResponseDTO> checkEmailVerification(
            @RequestParam String email) {

        EmailVerificationResponseDTO response = authService.checkEmailVerification(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Auth servisinin çalışıp çalışmadığını kontrol eder.
     */
    @Operation(summary = "Auth servis sağlık kontrolü")
    @GetMapping("/health")
    public String health() {
        return "Auth service is running";
    }

    /**
     * Kullanıcıya şifre sıfırlama e-postası gönderir.
     */
    @Operation(summary = "Şifre sıfırlama maili gönder")
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        PasswordResetResponseDTO response = authService.sendPasswordResetEmail(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Token ile şifre sıfırlama işlemini gerçekleştirir.
     */
    @Operation(summary = "Şifre sıfırla", description = "Token ile şifre sıfırlama işlemini gerçekleştirir")
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        PasswordResetResponseDTO response = authService.resetPassword(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kullanıcı girişi yapar. Başarılı girişte kullanıcı DB'ye kaydedilir.
     * OTP gerekliyse OTP_REQUIRED mesajı döner.
     */
    @Operation(summary = "Kullanıcı girişi", description = "Başarılı girişte access ve refresh token döner. 2FA aktifse OTP_REQUIRED mesajı döner")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login attempt for user: {}", request.getUsername());

        LoginResponseDTO response = authService.login(request);

        if (response.isSuccess()) {
            try {
                Jwt jwt = jwtDecoder.decode(response.getAccessToken());
                User user = userService.getOrCreateUser(jwt);
                log.info("✅ User ensured in DB: {} (keycloakId: {})", user.getUsername(), user.getKeycloakId());
            } catch (Exception e) {
                log.error("⚠️ Failed to save user to DB: {}", e.getMessage());
            }

            log.info("✅ Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
        }else if ("2FA_REQUIRED".equals(response.getMessage())) {
            log.info("⚠️ 2FA required for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Kullanıcının Keycloak oturumunu sonlandırır.
     */
    @Operation(summary = "Kullanıcı çıkışı", description = "Keycloak oturumunu sonlandırır")
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout(
            @Valid @RequestBody LogoutRequestDTO request) {

        LogoutResponseDTO response = authService.logout(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh token kullanarak yeni access token alır.
     */
    @Operation(summary = "Token yenile", description = "Refresh token kullanarak yeni access token alır")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {

        RefreshTokenResponseDTO response = authService.refreshToken(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Authorization code ile access token alır.
     * Başarılı olursa kullanıcı DB'ye kaydedilir.
     */
    @Operation(summary = "Token değişimi", description = "Authorization code ile access token alır")
    @PostMapping("/token-exchange")
    public ResponseEntity<LoginResponseDTO> tokenExchange(@RequestBody TokenExchangeRequestDTO request) {
        log.info("Token exchange request");

        LoginResponseDTO response = authService.exchangeCodeForToken(request.getCode());

        if (response.isSuccess()) {
            try {
                Jwt jwt = jwtDecoder.decode(response.getAccessToken());
                User user = userService.getOrCreateUser(jwt);
                log.info("✅ User ensured in DB after token exchange: {}", user.getUsername());
            } catch (Exception e) {
                log.error("⚠️ Failed to save user to DB after token exchange: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Email doğrulama tokenini doğrular.
     */
    @Operation(summary = "E-posta doğrula", description = "E-posta doğrulama tokenini doğrular")
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        log.info("Verifying email token");
        boolean success = emailService.verifyEmail(token);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Email doğrulandı"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Geçersiz veya süresi dolmuş token"));
        }
    }
}
