package com.financeportal.backend.PriceAlert;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.Util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAlert Service Unit Tests")
class PriceAlertServiceTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository instrumentPriceRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PriceAlertServiceImpl priceAlertService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";

    private BaseInstrument testInstrument;
    private PriceAlert testAlert;
    private CreatePriceAlertRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        testInstrument.setActive(true);

        testAlert = PriceAlert.builder()
                .userId(TEST_USER_ID)
                .instrument(testInstrument)
                .targetPrice(new BigDecimal("40.00"))
                .condition(AlertCondition.ABOVE)
                .active(true)
                .triggered(false)
                .build();

        createRequest = new CreatePriceAlertRequestDTO();
        createRequest.setInstrumentId(1L);
        createRequest.setTargetPrice(new BigDecimal("40.00"));
        createRequest.setCondition(AlertCondition.ABOVE);
    }

    // ========== CREATE ALERT TESTS ==========

    @Test
    @DisplayName("Fiyat alarmı başarıyla oluşturulmalı")
    void createAlert_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(priceAlertRepository.save(any(PriceAlert.class))).thenReturn(testAlert);

            PriceAlertDTO result = priceAlertService.createAlert(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getInstrumentSymbol()).isEqualTo("USD/TRY");
            assertThat(result.getTargetPrice()).isEqualByComparingTo(new BigDecimal("40.00"));
            assertThat(result.getCondition()).isEqualTo(AlertCondition.ABOVE);
            assertThat(result.isActive()).isTrue();
            assertThat(result.isTriggered()).isFalse();

            verify(priceAlertRepository, times(1)).save(any(PriceAlert.class));
        }
    }

    @Test
    @DisplayName("Var olmayan enstrüman için alarm oluşturulamaz")
    void createAlert_InstrumentNotFound_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());
            createRequest.setInstrumentId(999L);

            assertThatThrownBy(() -> priceAlertService.createAlert(createRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(priceAlertRepository, never()).save(any());
        }
    }

    // ========== GET ALERTS TESTS ==========

    @Test
    @DisplayName("Kullanıcının tüm alarmları getirilmeli")
    void getUserAlerts_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(priceAlertRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID))
                    .thenReturn(List.of(testAlert));

            List<PriceAlertDTO> result = priceAlertService.getUserAlerts();

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInstrumentSymbol()).isEqualTo("USD/TRY");
        }
    }

    @Test
    @DisplayName("Kullanıcının aktif alarmları getirilmeli")
    void getActiveUserAlerts_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(priceAlertRepository.findByUserIdAndActiveTrue(TEST_USER_ID))
                    .thenReturn(List.of(testAlert));

            List<PriceAlertDTO> result = priceAlertService.getActiveUserAlerts();

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }
    }

    @Test
    @DisplayName("Alarm başarıyla silinmeli")
    void deleteAlert_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            doNothing().when(priceAlertRepository).deleteByIdAndUserId(1L, TEST_USER_ID);

            priceAlertService.deleteAlert(1L);

            verify(priceAlertRepository, times(1)).deleteByIdAndUserId(1L, TEST_USER_ID);
        }
    }

    // ========== CHECK ALERTS TESTS ==========

    @Test
    @DisplayName("ABOVE koşulunda fiyat hedefin üzerindeyse alarm tetiklenmeli")
    void checkAlerts_AboveCondition_Triggered() {
        testAlert.setCondition(AlertCondition.ABOVE);
        testAlert.setTargetPrice(new BigDecimal("40.00"));

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("42.00"))
                .openPrice(BigDecimal.ZERO)
                .highPrice(BigDecimal.ZERO)
                .lowPrice(BigDecimal.ZERO)
                .previousClose(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();

        when(priceAlertRepository.findByActiveTrue()).thenReturn(List.of(testAlert));
        when(instrumentPriceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(price));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenReturn(testAlert);

        priceAlertService.checkAlerts();

        assertThat(testAlert.isTriggered()).isTrue();
        assertThat(testAlert.isActive()).isFalse();
        assertThat(testAlert.getTriggeredAt()).isNotNull();
        verify(notificationService, times(1)).notifyPriceAlert(
                eq(TEST_USER_ID), eq("USD/TRY"), anyDouble(), anyString()
        );
    }

    @Test
    @DisplayName("BELOW koşulunda fiyat hedefin altındaysa alarm tetiklenmeli")
    void checkAlerts_BelowCondition_Triggered() {
        testAlert.setCondition(AlertCondition.BELOW);
        testAlert.setTargetPrice(new BigDecimal("40.00"));

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("38.00"))
                .openPrice(BigDecimal.ZERO)
                .highPrice(BigDecimal.ZERO)
                .lowPrice(BigDecimal.ZERO)
                .previousClose(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();

        when(priceAlertRepository.findByActiveTrue()).thenReturn(List.of(testAlert));
        when(instrumentPriceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(price));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenReturn(testAlert);

        priceAlertService.checkAlerts();

        assertThat(testAlert.isTriggered()).isTrue();
        assertThat(testAlert.isActive()).isFalse();
        verify(notificationService, times(1)).notifyPriceAlert(
                eq(TEST_USER_ID), eq("USD/TRY"), anyDouble(), anyString()
        );
    }

    @Test
    @DisplayName("Fiyat koşulu sağlanmıyorsa alarm tetiklenmemeli")
    void checkAlerts_ConditionNotMet_NotTriggered() {
        testAlert.setCondition(AlertCondition.ABOVE);
        testAlert.setTargetPrice(new BigDecimal("40.00"));

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("38.00"))
                .openPrice(BigDecimal.ZERO)
                .highPrice(BigDecimal.ZERO)
                .lowPrice(BigDecimal.ZERO)
                .previousClose(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();

        when(priceAlertRepository.findByActiveTrue()).thenReturn(List.of(testAlert));
        when(instrumentPriceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(price));

        priceAlertService.checkAlerts();

        assertThat(testAlert.isTriggered()).isFalse();
        assertThat(testAlert.isActive()).isTrue();
        verify(notificationService, never()).notifyPriceAlert(any(), any(), anyDouble(), any());
    }

    @Test
    @DisplayName("Fiyat bilgisi yoksa alarm tetiklenmemeli")
    void checkAlerts_NoPriceData_NotTriggered() {
        when(priceAlertRepository.findByActiveTrue()).thenReturn(List.of(testAlert));
        when(instrumentPriceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.empty());

        priceAlertService.checkAlerts();

        assertThat(testAlert.isTriggered()).isFalse();
        verify(notificationService, never()).notifyPriceAlert(any(), any(), anyDouble(), any());
    }
}
