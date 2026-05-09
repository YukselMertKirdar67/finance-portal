package com.financeportal.backend.Totp;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class TotpService {

    private final TotpSecretRepository totpSecretRepository;

    /**
     * Kullanıcı için yeni TOTP secret üretir ve QR kod döner.
     * Daha önce kurulmamışsa yeni secret oluşturur.
     */
    @Transactional
    public Map<String, String> setupTotp(String keycloakId, String email) {
        log.info("Setting up TOTP for user: {}", keycloakId);

        // Eski secret varsa sil
        totpSecretRepository.deleteByKeycloakId(keycloakId);

        // Yeni secret üret
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        // DB'ye kaydet (henüz verified değil)
        TotpSecret totpSecret = TotpSecret.builder()
                .keycloakId(keycloakId)
                .secret(secret)
                .verified(false)
                .build();
        totpSecretRepository.save(totpSecret);

        // QR kod üret
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("FinansApp")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(qrData);
            String qrCodeBase64 = Base64.getEncoder().encodeToString(imageData);

            log.info("✅ TOTP setup initiated for user: {}", keycloakId);

            return Map.of(
                    "secret", secret,
                    "qrCode", "data:image/png;base64," + qrCodeBase64
            );
        } catch (QrGenerationException e) {
            log.error("❌ QR code generation failed: {}", e.getMessage());
            throw new RuntimeException("QR kod oluşturulamadı");
        }
    }

    /**
     * Kullanıcının girdiği kodu doğrular ve 2FA'yı aktif eder.
     */
    @Transactional
    public boolean verifyAndActivateTotp(String keycloakId, String code) {
        log.info("Verifying TOTP code for user: {}", keycloakId);

        TotpSecret totpSecret = totpSecretRepository.findByKeycloakId(keycloakId)
                .orElse(null);

        if (totpSecret == null) {
            log.warn("No TOTP secret found for user: {}", keycloakId);
            return false;
        }

        boolean isValid = verifyCode(totpSecret.getSecret(), code);

        if (isValid) {
            totpSecret.setVerified(true);
            totpSecretRepository.save(totpSecret);
            log.info("✅ TOTP activated for user: {}", keycloakId);
        } else {
            log.warn("❌ Invalid TOTP code for user: {}", keycloakId);
        }

        return isValid;
    }

    /**
     * Login sırasında TOTP kodunu doğrular.
     */
    public boolean verifyTotpCode(String keycloakId, String code) {
        log.info("Verifying TOTP login code for user: {}", keycloakId);

        TotpSecret totpSecret = totpSecretRepository.findByKeycloakId(keycloakId)
                .orElse(null);

        if (totpSecret == null || !totpSecret.isVerified()) {
            log.warn("No verified TOTP found for user: {}", keycloakId);
            return false;
        }

        boolean isValid = verifyCode(totpSecret.getSecret(), code);
        log.info("TOTP verification result for user {}: {}", keycloakId, isValid);
        return isValid;
    }

    /**
     * Kullanıcının 2FA aktif olup olmadığını kontrol eder.
     */
    public boolean isTotpEnabled(String keycloakId) {
        return totpSecretRepository.existsByKeycloakIdAndVerifiedTrue(keycloakId);
    }

    /**
     * Kullanıcının 2FA'sını devre dışı bırakır.
     */
    @Transactional
    public void disableTotp(String keycloakId) {
        log.info("Disabling TOTP for user: {}", keycloakId);
        totpSecretRepository.deleteByKeycloakId(keycloakId);
        log.info("✅ TOTP disabled for user: {}", keycloakId);
    }

    /**
     * TOTP kodunu doğrular.
     */
    private boolean verifyCode(String secret, String code) {
        try {
            TimeProvider timeProvider = new SystemTimeProvider();
            CodeGenerator codeGenerator = new DefaultCodeGenerator();
            CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
            return verifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.error("Error verifying TOTP code: {}", e.getMessage());
            return false;
        }
    }
}
