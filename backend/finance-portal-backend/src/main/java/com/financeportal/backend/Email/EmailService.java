package com.financeportal.backend.Email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    /**
     * Email doğrulama tokeni oluşturur ve mail gönderir.
     */
    @Transactional
    public void sendVerificationEmail(String keycloakId, String email) {
        log.info("Sending verification email to: {}", email);

        // Eski tokenları sil
        tokenRepository.deleteByKeycloakId(keycloakId);

        // Yeni token oluştur
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .keycloakId(keycloakId)
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);

        // Email gönder
        String verificationUrl = "http://localhost:3000/email-verified?token=" + token;
        sendEmail(email, verificationUrl);

        log.info("✅ Verification email sent to: {}", email);
    }

    /**
     * Token doğrular ve Keycloak'ta emailVerified = true yapar.
     */
    @Transactional
    public boolean verifyEmail(String token) {
        log.info("Verifying email token: {}", token);

        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (verificationToken == null) {
            log.warn("Token not found: {}", token);
            return false;
        }

        if (verificationToken.isUsed()) {
            log.warn("Token already used: {}", token);
            return false;
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Token expired: {}", token);
            return false;
        }

        // Keycloak'ta emailVerified = true yap
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserRepresentation user = realmResource.users()
                    .get(verificationToken.getKeycloakId())
                    .toRepresentation();
            user.setEmailVerified(true);
            realmResource.users()
                    .get(verificationToken.getKeycloakId())
                    .update(user);

            log.info("✅ Email verified in Keycloak for: {}", verificationToken.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to update Keycloak: {}", e.getMessage());
            return false;
        }

        // Token'ı kullanıldı olarak işaretle
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("✅ Email verified successfully: {}", verificationToken.getEmail());
        return true;
    }

    /**
     * HTML email gönderir.
     */
    private void sendEmail(String to, String verificationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@financeportal.com");
            helper.setTo(to);
            helper.setSubject("FinansApp - E-posta Doğrulama");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <div style="background: linear-gradient(135deg, #2563eb, #1d4ed8); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                            <h1 style="color: white; margin: 0;">FinansApp</h1>
                        </div>
                        <div style="background: #f8fafc; padding: 30px; border-radius: 0 0 10px 10px;">
                            <h2 style="color: #1e293b;">E-posta Adresinizi Doğrulayın</h2>
                            <p style="color: #64748b;">Hesabınızı aktifleştirmek için aşağıdaki butona tıklayın.</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s"
                                   style="background: #2563eb; color: white; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: bold; font-size: 16px;">
                                    E-postamı Doğrula
                                </a>
                            </div>
                            <p style="color: #94a3b8; font-size: 12px;">Bu link 24 saat geçerlidir.</p>
                            <p style="color: #94a3b8; font-size: 12px;">Eğer bu isteği siz yapmadıysanız bu emaili görmezden gelebilirsiniz.</p>
                        </div>
                    </div>
                    """.formatted(verificationUrl);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            log.error("❌ Failed to send email: {}", e.getMessage());
            throw new RuntimeException("Email gönderilemedi");
        }
    }
}
