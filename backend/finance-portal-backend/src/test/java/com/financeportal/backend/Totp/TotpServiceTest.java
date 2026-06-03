package com.financeportal.backend.Totp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Totp Service Unit Tests")
class TotpServiceTest {

    @Mock
    private TotpSecretRepository totpSecretRepository;

    @InjectMocks
    private TotpService totpService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";
    private TotpSecret testTotpSecret;

    @BeforeEach
    void setUp() {
        testTotpSecret = TotpSecret.builder()
                .keycloakId(TEST_USER_ID)
                .secret("JBSWY3DPEHPK3PXP")
                .verified(true)
                .build();
    }

    @Test
    @DisplayName("TOTP kurulumu başarıyla yapılmalı")
    void setupTotp_Success() {
        doNothing().when(totpSecretRepository).deleteByKeycloakId(TEST_USER_ID);
        when(totpSecretRepository.save(any(TotpSecret.class))).thenReturn(testTotpSecret);

        Map<String, String> result = totpService.setupTotp(TEST_USER_ID, "test@test.com");

        assertThat(result).isNotNull();
        assertThat(result).containsKey("secret");
        assertThat(result).containsKey("qrCode");
        assertThat(result.get("secret")).isNotEmpty();
        assertThat(result.get("qrCode")).startsWith("data:image/png;base64,");
        verify(totpSecretRepository, times(1)).deleteByKeycloakId(TEST_USER_ID);
        verify(totpSecretRepository, times(1)).save(any(TotpSecret.class));
    }

    @Test
    @DisplayName("TOTP secret yoksa doğrulama false dönmeli")
    void verifyAndActivateTotp_NoSecret_ReturnsFalse() {
        when(totpSecretRepository.findByKeycloakId(TEST_USER_ID)).thenReturn(Optional.empty());

        boolean result = totpService.verifyAndActivateTotp(TEST_USER_ID, "123456");

        assertThat(result).isFalse();
        verify(totpSecretRepository, never()).save(any());
    }

    @Test
    @DisplayName("TOTP aktif değilse login doğrulaması false dönmeli")
    void verifyTotpCode_NotVerified_ReturnsFalse() {
        testTotpSecret.setVerified(false);
        when(totpSecretRepository.findByKeycloakId(TEST_USER_ID)).thenReturn(Optional.of(testTotpSecret));

        boolean result = totpService.verifyTotpCode(TEST_USER_ID, "123456");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("TOTP secret yoksa login doğrulaması false dönmeli")
    void verifyTotpCode_NoSecret_ReturnsFalse() {
        when(totpSecretRepository.findByKeycloakId(TEST_USER_ID)).thenReturn(Optional.empty());

        boolean result = totpService.verifyTotpCode(TEST_USER_ID, "123456");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("TOTP aktifse isTotpEnabled true dönmeli")
    void isTotpEnabled_ReturnsTrue() {
        when(totpSecretRepository.existsByKeycloakIdAndVerifiedTrue(TEST_USER_ID)).thenReturn(true);

        boolean result = totpService.isTotpEnabled(TEST_USER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("TOTP aktif değilse isTotpEnabled false dönmeli")
    void isTotpEnabled_ReturnsFalse() {
        when(totpSecretRepository.existsByKeycloakIdAndVerifiedTrue(TEST_USER_ID)).thenReturn(false);

        boolean result = totpService.isTotpEnabled(TEST_USER_ID);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("TOTP devre dışı bırakılmalı")
    void disableTotp_Success() {
        doNothing().when(totpSecretRepository).deleteByKeycloakId(TEST_USER_ID);

        totpService.disableTotp(TEST_USER_ID);

        verify(totpSecretRepository, times(1)).deleteByKeycloakId(TEST_USER_ID);
    }
}
