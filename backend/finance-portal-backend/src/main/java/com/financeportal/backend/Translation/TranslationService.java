package com.financeportal.backend.Translation;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Log4j2
public class TranslationService {

    @Value("${libretranslate.url:http://finance-libretranslate:5000}")
    private String libreTranslateUrl;

    private final RestTemplate restTemplate;

    public TranslationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Metni kaynak dilden hedef dile çevirir.
     * Hata durumunda orijinal metni döner.
     */
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) return text;
        if (sourceLang.equals(targetLang)) return text;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of(
                    "q", text,
                    "source", sourceLang,
                    "target", targetLang,
                    "format", "text"
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    libreTranslateUrl + "/translate",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String translated = (String) response.getBody().get("translatedText");
                return translated != null ? translated : text;
            }

        } catch (Exception e) {
            log.warn("Translation failed for text: {} | Error: {}",
                    text.substring(0, Math.min(50, text.length())), e.getMessage());
        }

        return text; // Çeviri başarısız olursa orijinal metni döner
    }

    /**
     * Türkçe'den İngilizce'ye çevirir.
     */
    public String translateToEnglish(String text) {
        return translate(text, "tr", "en");
    }

    /**
     * İngilizce'den Türkçe'ye çevirir.
     */
    public String translateToTurkish(String text) {
        return translate(text, "en", "tr");
    }
}
