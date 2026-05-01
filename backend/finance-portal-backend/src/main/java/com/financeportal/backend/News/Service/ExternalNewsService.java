package com.financeportal.backend.News.Service;

import com.financeportal.backend.News.DTO.External.ExternalNewsResponse;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Repository.NewsRepository;
import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.User.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExternalNewsService {

    @Value("${newsapi.api.key}")
    private String apiKey;

    @Value("${newsapi.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final NewsRepository newsRepository;
    private final ImageService imageService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ExternalNewsService(RestTemplate restTemplate, NewsRepository newsRepository, ImageService imageService, NotificationService notificationService, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.newsRepository = newsRepository;
        this.imageService = imageService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Caching(evict = {
            @CacheEvict(value = "news", allEntries = true),
            @CacheEvict(value = "allNews", allEntries = true),
            @CacheEvict(value = "newsByCategory", allEntries = true)
    })
    public Map<String, Object> fetchAndSaveFinanceNews() {
        Map<String, Integer> categoryResults = new LinkedHashMap<>();
        Map<String, Integer> skippedResults = new LinkedHashMap<>();
        int totalSaved = 0;
        int totalFetched = 0;
        int totalSkipped = 0;

        // Daha spesifik keyword'ler
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("borsa hisse piyasa", "FINANS");
        categories.put("dolar euro kur", "DOVIZ");
        categories.put("bitcoin ethereum kripto", "KRIPTO");

        log.info("Starting to fetch news from NewsAPI...");

        for (Map.Entry<String, String> entry : categories.entrySet()) {
            String query = entry.getKey();
            String displayCategory = entry.getValue();

            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = apiUrl
                        + "?q=" + encodedQuery
                        + "&language=tr"
                        + "&sortBy=publishedAt"
                        + "&pageSize=20"
                        + "&apiKey=" + apiKey;

                log.info("Fetching {} news...", displayCategory);

                ExternalNewsResponse response =
                        restTemplate.getForObject(url, ExternalNewsResponse.class);

                if (response == null || response.getArticles() == null || response.getArticles().isEmpty()) {
                    log.warn("No {} articles found", displayCategory);
                    categoryResults.put(displayCategory, 0);
                    skippedResults.put(displayCategory, 0);
                    continue;
                }

                List<ExternalNewsResponse.Article> articles = response.getArticles();
                totalFetched += articles.size();
                log.info("Found {} {} articles", articles.size(), displayCategory);

                int savedCount = 0;
                int skippedCount = 0;

                for (ExternalNewsResponse.Article article : articles) {

                    // 1. BAŞLIK KONTROLÜ
                    if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
                        skippedCount++;
                        log.debug("Skipped: No title");
                        continue;
                    }

                    if (article.getTitle().equalsIgnoreCase("[Removed]")) {
                        skippedCount++;
                        log.debug("Skipped: Removed article");
                        continue;
                    }

                    // 2. İÇERİK KONTROLÜ
                    if (!hasValidContent(article)) {
                        skippedCount++;
                        log.debug("Skipped: No valid content - {}", article.getTitle());
                        continue;
                    }

                    // 3. İLGİLİLİK KONTROLÜ
                    if (!isRelevantFinanceNews(displayCategory, article)) {
                        skippedCount++;
                        log.debug("Skipped: Not relevant - {}", article.getTitle());
                        continue;
                    }

                    // 4. DUPLICATE KONTROLÜ
                    String sourceName = article.getSource() != null
                            ? article.getSource().getName()
                            : "Unknown";

                    if (newsRepository.existsByTitleAndSource(article.getTitle(), sourceName)) {
                        skippedCount++;
                        log.debug("Skipped: Duplicate - {}", article.getTitle());
                        continue;
                    }

                    // 5. KAYIT
                    News news = new News();
                    news.setTitle(article.getTitle());
                    news.setContent(getContent(article));
                    news.setSource(sourceName);
                    news.setCategory(displayCategory);
                    news.setImageUrl(imageService.getImageUrl(article.getUrlToImage(), displayCategory));

                    if (article.getPublishedAt() != null) {
                        news.setPublishDate(LocalDateTime.parse(
                                article.getPublishedAt(),
                                DateTimeFormatter.ISO_DATE_TIME
                        ));
                    } else {
                        news.setPublishDate(LocalDateTime.now());
                    }

                    try {
                        newsRepository.save(news);
                        savedCount++;
                        log.debug("Saved: {} - {}", displayCategory, article.getTitle());

                        if (savedCount == 1) {
                            List<String> allUserIds = userRepository.findAllKeycloakIds();
                            for (String userId : allUserIds) {
                                notificationService.notifyNews(userId, news.getTitle(), news.getId());
                            }
                        }
                    } catch (Exception e) {
                        skippedCount++;
                        log.error("Error saving article: {}", article.getTitle(), e);
                    }
                }

                categoryResults.put(displayCategory, savedCount);
                skippedResults.put(displayCategory, skippedCount);
                totalSaved += savedCount;
                totalSkipped += skippedCount;

                log.info("Category {}: Saved={}, Skipped={}",
                        displayCategory, savedCount, skippedCount);

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted", e);
                categoryResults.put(displayCategory, 0);
                skippedResults.put(displayCategory, 0);
            } catch (Exception e) {
                log.error("Error fetching {} news: {}", displayCategory, e.getMessage(), e);
                categoryResults.put(displayCategory, 0);
                skippedResults.put(displayCategory, 0);
            }
        }

        log.info("Total - Fetched: {}, Saved: {}, Skipped: {}",
                totalFetched, totalSaved, totalSkipped);

        return Map.of(
                "totalFetched", totalFetched,
                "totalSaved", totalSaved,
                "totalSkipped", totalSkipped,
                "saved", categoryResults,
                "skipped", skippedResults
        );
    }

    //İçerik filtreleme
    private boolean isRelevantFinanceNews(String category, ExternalNewsResponse.Article article) {
        String text = ((article.getTitle() != null ? article.getTitle() : "") + " " +
                (article.getDescription() != null ? article.getDescription() : "")).toLowerCase();

        return switch (category) {
            case "FINANS" -> text.contains("borsa") || text.contains("hisse") ||
                    text.contains("bist") || text.contains("ekonomi") ||
                    text.contains("piyasa") || text.contains("yatırım") ||
                    text.contains("faiz") || text.contains("enflasyon");
            case "DOVIZ" -> text.contains("döviz") || text.contains("kur") ||
                    text.contains("dolar") || text.contains("euro") ||
                    text.contains("merkez bankası") || text.contains("tcmb") ||
                    text.contains("sterlin") || text.contains("forex");
            case "KRIPTO" -> text.contains("bitcoin") || text.contains("kripto") ||
                    text.contains("ethereum") || text.contains("blockchain") ||
                    text.contains("btc") || text.contains("eth") ||
                    text.contains("altcoin") || text.contains("token");
            default -> true;
        };
    }

    private String getContent(ExternalNewsResponse.Article article) {
        String content = article.getDescription() != null
                ? article.getDescription()
                : article.getContent();

        if (content == null) return "";

        return content.replaceAll("\\[\\+\\d+ chars\\]", "").trim();
    }

    private boolean hasValidContent(ExternalNewsResponse.Article article) {
        String content = article.getDescription() != null
                ? article.getDescription()
                : article.getContent();

        if (content == null || content.trim().isEmpty()) return false;
        if (content.trim().length() < 50) return false;

        String[] words = content.trim().split("\\s+");
        if (words.length < 10) return false;

        if (content.trim().replaceAll("\\s+", "").isEmpty()) return false;

        String lower = content.toLowerCase();
        if (lower.contains("no content") ||
                lower.contains("not available") ||
                lower.contains("content not found") ||
                lower.equals("null") ||
                lower.equals("n/a")) {
            return false;
        }

        return true;
    }
}