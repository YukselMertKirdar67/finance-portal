package com.financeportal.backend.Instrument;

import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import com.financeportal.backend.Instrument.Service.TcmbEvdsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TcmbEvdsService Unit Testleri")
class TcmbEvdsServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private InstrumentPriceRepository priceRepository;
    @Mock private PriceHistoryRepository historyRepository;

    @InjectMocks
    private TcmbEvdsService tcmbEvdsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tcmbEvdsService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(tcmbEvdsService, "evdsUrl",
                "https://evds2.tcmb.gov.tr/service/evds/");
    }

    @Test
    @DisplayName("API key boşsa boş liste dönmeli")
    void fetchBondYields_EmptyApiKey_ReturnsEmpty() {
        ReflectionTestUtils.setField(tcmbEvdsService, "apiKey", "");

        List<InstrumentPrice> result = tcmbEvdsService.fetchBondYields();

        assertThat(result).isEmpty();
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Bağlantı testi başarısız olduğunda boş liste dönmeli")
    void fetchBondYields_ConnectionFailed_ReturnsEmpty() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        List<InstrumentPrice> result = tcmbEvdsService.fetchBondYields();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("HTML response geldiğinde boş liste dönmeli")
    void fetchBondYields_HtmlResponse_ReturnsEmpty() {
        ResponseEntity<String> htmlResponse = ResponseEntity.ok(
                "<html><body>Unauthorized</body></html>");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(htmlResponse);

        List<InstrumentPrice> result = tcmbEvdsService.fetchBondYields();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Geçmiş veri çekme - API key boşsa işlem yapılmamalı")
    void fetchBondYieldsHistorical_EmptyApiKey_DoesNothing() {
        ReflectionTestUtils.setField(tcmbEvdsService, "apiKey", "");

        assertThatNoException().isThrownBy(() ->
                tcmbEvdsService.fetchBondYieldsHistorical(
                        LocalDate.now().minusDays(30), LocalDate.now()));

        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Geçmiş veri çekme - exception durumunda hata fırlatmamalı")
    void fetchBondYieldsHistorical_Exception_DoesNotThrow() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        assertThatNoException().isThrownBy(() ->
                tcmbEvdsService.fetchBondYieldsHistorical(
                        LocalDate.now().minusDays(7), LocalDate.now()));
    }

    @Test
    @DisplayName("Bağlantı testi 403 döndüğünde false dönmeli")
    void fetchBondYields_403Response_ReturnsEmpty() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("403 Forbidden"));

        List<InstrumentPrice> result = tcmbEvdsService.fetchBondYields();

        assertThat(result).isEmpty();
    }
}
