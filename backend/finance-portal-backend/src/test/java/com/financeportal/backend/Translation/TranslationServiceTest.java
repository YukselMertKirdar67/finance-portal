package com.financeportal.backend.Translation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(translationService, "libreTranslateUrl", "http://localhost:5000");
    }

    // ==================== translate() ====================

    @Test
    void translate_Success_ReturnsTranslatedText() {
        // Given
        String originalText = "Borsa yükseliyor";
        String expectedTranslation = "The stock market is rising";

        Map<String, Object> responseBody = Map.of("translatedText", expectedTranslation);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = translationService.translate(originalText, "tr", "en");

        // Then
        assertThat(result).isEqualTo(expectedTranslation);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void translate_SameLanguage_ReturnsOriginalWithoutApiCall() {
        // Given
        String text = "Borsa haberleri";

        // When
        String result = translationService.translate(text, "tr", "tr");

        // Then
        assertThat(result).isEqualTo(text);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void translate_NullText_ReturnsNull() {
        // When
        String result = translationService.translate(null, "tr", "en");

        // Then
        assertThat(result).isNull();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void translate_BlankText_ReturnsBlank() {
        // When
        String result = translationService.translate("   ", "tr", "en");

        // Then
        assertThat(result).isBlank();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void translate_EmptyText_ReturnsEmpty() {
        // When
        String result = translationService.translate("", "tr", "en");

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void translate_LibreTranslateDown_ReturnsOriginalText() {
        // Given
        String originalText = "Dolar kuru yükseldi";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // When
        String result = translationService.translate(originalText, "tr", "en");

        // Then
        assertThat(result).isEqualTo(originalText);
    }

    @Test
    void translate_LibreTranslateTimeout_ReturnsOriginalText() {
        // Given
        String originalText = "Kripto piyasası";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Read timed out"));

        // When
        String result = translationService.translate(originalText, "tr", "en");

        // Then
        assertThat(result).isEqualTo(originalText);
    }

    @Test
    void translate_ApiReturns5xx_ReturnsOriginalText() {
        // Given
        String originalText = "Altın fiyatları";

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = translationService.translate(originalText, "tr", "en");

        // Then
        assertThat(result).isEqualTo(originalText);
    }

    @Test
    void translate_ApiReturnsNullBody_ReturnsOriginalText() {
        // Given
        String originalText = "Hisse senedi analizi";

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = translationService.translate(originalText, "tr", "en");

        // Then
        assertThat(result).isEqualTo(originalText);
    }

    @Test
    void translate_ApiReturnsNullTranslatedText_ReturnsOriginalText() {
        // Given
        String originalText = "Tahvil piyasası";

        Map<String, Object> responseBody = new java.util.HashMap<>();
        responseBody.put("translatedText", null);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = translationService.translate(originalText, "tr", "en");

        // Then
        assertThat(result).isEqualTo(originalText);
    }

    @Test
    void translate_CorrectUrlCalled() {
        // Given
        String text = "Test metni";
        Map<String, Object> responseBody = Map.of("translatedText", "Test text");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        translationService.translate(text, "tr", "en");

        // Then — doğru URL'ye istek atılıyor mu
        verify(restTemplate).postForEntity(
                eq("http://localhost:5000/translate"),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    // ==================== translateToEnglish() ====================

    @Test
    void translateToEnglish_Success_ReturnsEnglishText() {
        // Given
        String turkishText = "Finansal haberler";
        String englishText = "Financial news";

        Map<String, Object> responseBody = Map.of("translatedText", englishText);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = translationService.translateToEnglish(turkishText);

        // Then
        assertThat(result).isEqualTo(englishText);
    }

    @Test
    void translateToEnglish_Failure_ReturnsOriginalText() {
        // Given
        String turkishText = "Döviz haberleri";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        String result = translationService.translateToEnglish(turkishText);

        // Then
        assertThat(result).isEqualTo(turkishText);
    }

    // ==================== translateToTurkish() ====================

    @Test
    void translateToTurkish_Success_ReturnsTurkishText() {
        // Given
        String englishText = "Stock market news";
        String turkishText = "Borsa haberleri";

        Map<String, Object> responseBody = Map.of("translatedText", turkishText);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = translationService.translateToTurkish(englishText);

        // Then
        assertThat(result).isEqualTo(turkishText);
    }

    @Test
    void translateToTurkish_Failure_ReturnsOriginalText() {
        // Given
        String englishText = "Crypto market update";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        // When
        String result = translationService.translateToTurkish(englishText);

        // Then
        assertThat(result).isEqualTo(englishText);
    }
}
