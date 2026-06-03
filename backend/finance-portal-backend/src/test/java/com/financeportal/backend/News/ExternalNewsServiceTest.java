package com.financeportal.backend.News;

import com.financeportal.backend.News.DTO.External.ExternalNewsResponse;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Repository.NewsRepository;
import com.financeportal.backend.News.Service.ExternalNewsService;
import com.financeportal.backend.News.Service.ImageService;
import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.User.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExternalNewsService Unit Testleri")
class ExternalNewsServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private NewsRepository newsRepository;
    @Mock private ImageService imageService;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ExternalNewsService externalNewsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(externalNewsService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(externalNewsService, "apiUrl", "https://newsapi.org/v2/everything");

        when(imageService.getImageUrl(any(), anyString())).thenReturn("https://test-image.com");
        when(newsRepository.existsByTitleAndSource(anyString(), anyString())).thenReturn(false);
        when(userRepository.findAllKeycloakIds()).thenReturn(List.of());
        doNothing().when(notificationService).notifyNews(anyString(), anyString(), any());
    }

    // ========== fetchAndSaveFinanceNews ==========

    @Test
    @DisplayName("API null döndürdüğünde sıfır kayıt dönmeli")
    void fetchAndSaveFinanceNews_NullResponse_ReturnsZero() {
        when(restTemplate.getForObject(anyString(), eq(ExternalNewsResponse.class))).thenReturn(null);

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result).isNotNull();
        assertThat(result.get("totalSaved")).isEqualTo(0);
        assertThat(result.get("totalFetched")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Boş makale listesi döndüğünde sıfır kayıt dönmeli")
    void fetchAndSaveFinanceNews_EmptyArticles_ReturnsZero() {
        ExternalNewsResponse emptyResponse = new ExternalNewsResponse();
        emptyResponse.setArticles(List.of());
        when(restTemplate.getForObject(anyString(), eq(ExternalNewsResponse.class)))
                .thenReturn(emptyResponse);

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Başlığı null olan makale atlanmalı")
    void fetchAndSaveFinanceNews_NullTitle_SkipsArticle() {
        ExternalNewsResponse.Article article = createArticle(null,
                "Bu bir finans haberidir borsa ve piyasalar hakkında detaylı bilgi içermektedir gerçekten uzun içerik",
                "Test Source");

        mockResponse(List.of(article));

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("[Removed] başlıklı makale atlanmalı")
    void fetchAndSaveFinanceNews_RemovedTitle_SkipsArticle() {
        ExternalNewsResponse.Article article = createArticle("[Removed]",
                "Bu bir finans haberidir borsa ve piyasalar hakkında detaylı bilgi içermektedir gerçekten uzun içerik",
                "Test Source");

        mockResponse(List.of(article));

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Çok kısa içerikli makale atlanmalı")
    void fetchAndSaveFinanceNews_ShortContent_SkipsArticle() {
        ExternalNewsResponse.Article article = createArticle(
                "Borsa haberi", "Kısa içerik", "Test Source");

        mockResponse(List.of(article));

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("İlgisiz içerikli makale atlanmalı")
    void fetchAndSaveFinanceNews_IrrelevantContent_SkipsArticle() {
        ExternalNewsResponse.Article article = createArticle(
                "Spor haberi",
                "Bu bir spor haberidir futbol maçı hakkında detaylı bilgi içermektedir çok uzun bir içerik mevcuttur",
                "Spor Source");

        mockResponse(List.of(article));

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate makale atlanmalı")
    void fetchAndSaveFinanceNews_DuplicateArticle_SkipsArticle() {
        ExternalNewsResponse.Article article = createArticle(
                "Borsa haberi",
                "Bu bir borsa haberidir piyasalar hakkında detaylı bilgi içermektedir ekonomi ve yatırım",
                "Test Source");

        mockResponse(List.of(article));
        when(newsRepository.existsByTitleAndSource(anyString(), anyString())).thenReturn(true);

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Geçerli FINANS makalesi kaydedilmeli")
    void fetchAndSaveFinanceNews_ValidFinansArticle_SavesArticle() {
        ExternalNewsResponse.Article article = createArticle(
                "Borsa güne yükselişle başladı",
                "Türkiye borsası BIST 100 endeksi bugün piyasa açılışında yükseliş kaydetti ekonomi ve yatırım haberleri",
                "Bloomberg HT");
        article.setPublishedAt("2024-01-15T10:00:00Z");

        mockResponse(List.of(article));
        when(newsRepository.save(any(News.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat((int) result.get("totalSaved")).isGreaterThanOrEqualTo(1);
        verify(newsRepository, atLeastOnce()).save(any(News.class));
    }

    @Test
    @DisplayName("Sonuç map doğru anahtarları içermeli")
    void fetchAndSaveFinanceNews_ReturnsCorrectKeys() {
        when(restTemplate.getForObject(anyString(), eq(ExternalNewsResponse.class))).thenReturn(null);

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result).containsKeys(
                "totalFetched", "totalSaved", "totalSkipped", "saved", "skipped");
    }

    @Test
    @DisplayName("API exception olduğunda sıfır kayıt dönmeli")
    void fetchAndSaveFinanceNews_ApiException_ReturnsZero() {
        when(restTemplate.getForObject(anyString(), eq(ExternalNewsResponse.class)))
                .thenThrow(new RuntimeException("API bağlantı hatası"));

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

        assertThat(result.get("totalSaved")).isEqualTo(0);
        verify(newsRepository, never()).save(any());
    }

    // ========== HELPER METHODS ==========

    private ExternalNewsResponse.Article createArticle(String title, String description, String sourceName) {
        ExternalNewsResponse.Article article = new ExternalNewsResponse.Article();
        article.setTitle(title);
        article.setDescription(description);

        ExternalNewsResponse.Source source = new ExternalNewsResponse.Source();
        source.setName(sourceName);
        article.setSource(source);

        return article;
    }

    private void mockResponse(List<ExternalNewsResponse.Article> articles) {
        ExternalNewsResponse response = new ExternalNewsResponse();
        response.setArticles(articles);
        when(restTemplate.getForObject(anyString(), eq(ExternalNewsResponse.class)))
                .thenReturn(response);
    }
}
