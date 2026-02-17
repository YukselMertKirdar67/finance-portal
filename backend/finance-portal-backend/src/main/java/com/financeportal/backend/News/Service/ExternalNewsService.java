package com.financeportal.backend.News.Service;

import com.financeportal.backend.News.DTO.External.ExternalNewsResponse;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Repository.NewsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class ExternalNewsService {

    @Value("${finance.api.key}")
    private String apiKey;

    @Value("${finance.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final NewsRepository newsRepository;
    private final ImageService imageService;

    public ExternalNewsService(RestTemplate restTemplate, NewsRepository newsRepository, ImageService imageService) {
        this.restTemplate = restTemplate;
        this.newsRepository = newsRepository;
        this.imageService = imageService;
    }

    /**
     * Tüm kategorilerden haberleri otomatik çek ve kaydet
     * ✅ CACHE TEMİZLİĞİ EKLENDİ
     */
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

        // Finnhub kategorileri
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("general", "FINANS");
        categories.put("forex", "DOVIZ");
        categories.put("crypto", "KRIPTO");
        categories.put("merger", "BIRLESME");

        log.info("Starting to fetch news from all categories...");

        for (Map.Entry<String, String> entry : categories.entrySet()) {
            String apiCategory = entry.getKey();
            String displayCategory = entry.getValue();

            try {
                String url = apiUrl + "?category=" + apiCategory + "&token=" + apiKey;

                log.info("Fetching {} news...", displayCategory);

                ExternalNewsResponse.Article[] articlesArray =
                        restTemplate.getForObject(url, ExternalNewsResponse.Article[].class);

                if (articlesArray == null || articlesArray.length == 0) {
                    log.warn("No {} articles found", displayCategory);
                    categoryResults.put(displayCategory, 0);
                    skippedResults.put(displayCategory, 0);
                    continue;
                }

                totalFetched += articlesArray.length;
                log.info("Found {} {} articles", articlesArray.length, displayCategory);

                int savedCount = 0;
                int skippedCount = 0;

                for (ExternalNewsResponse.Article article : articlesArray) {

                    // 1. BAŞLIK KONTROLÜ
                    if (article.getHeadline() == null || article.getHeadline().trim().isEmpty()) {
                        skippedCount++;
                        log.debug("Skipped: No headline");
                        continue;
                    }

                    // 2. İÇERİK KONTROLÜ
                    if (!hasValidContent(article)) {
                        skippedCount++;
                        log.debug("Skipped: No valid content - {}", article.getHeadline());
                        continue;
                    }

                    // 3. DUPLICATE KONTROLÜ
                    if (newsRepository.existsByTitleAndSource(
                            article.getHeadline(),
                            article.getSource()
                    )) {
                        skippedCount++;
                        log.debug("Skipped: Duplicate - {}", article.getHeadline());
                        continue;
                    }

                    News news = new News();
                    news.setTitle(article.getHeadline());
                    news.setContent(article.getSummary());
                    news.setSource(article.getSource() != null ?
                            article.getSource() : "Unknown");
                    news.setCategory(displayCategory);
                    news.setImageUrl(imageService.getImageUrl(article.getImage(), displayCategory));

                    // Unix timestamp'i LocalDateTime'a çevir
                    if (article.getDatetime() != null) {
                        news.setPublishDate(LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(article.getDatetime()),
                                ZoneId.systemDefault()
                        ));
                    } else {
                        news.setPublishDate(LocalDateTime.now());
                    }

                    try {
                        newsRepository.save(news);
                        savedCount++;
                        log.debug("Saved: {} - {}", displayCategory, article.getHeadline());
                    } catch (Exception e) {
                        skippedCount++;
                        log.error("Error saving article: {}", article.getHeadline(), e);
                    }
                }

                categoryResults.put(displayCategory, savedCount);
                skippedResults.put(displayCategory, skippedCount);
                totalSaved += savedCount;
                totalSkipped += skippedCount;

                log.info("Category {}: Saved={}, Skipped={}",
                        displayCategory, savedCount, skippedCount);

                // API rate limit için kısa bekleme
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

    /**
     * İçeriğin geçerli olup olmadığını kontrol et
     */
    private boolean hasValidContent(ExternalNewsResponse.Article article) {
        String summary = article.getSummary();

        // İçerik null veya boş mu?
        if (summary == null || summary.trim().isEmpty()) {
            return false;
        }

        // Çok kısa içerik (minimum 50 karakter)
        if (summary.trim().length() < 50) {
            log.debug("Content too short ({}): {}", summary.length(), summary);
            return false;
        }

        // Minimum kelime sayısı (en az 10 kelime)
        String[] words = summary.trim().split("\\s+");
        if (words.length < 10) {
            log.debug("Not enough words ({}): {}", words.length, summary);
            return false;
        }

        // Sadece boşluk karakterleri mi?
        if (summary.trim().replaceAll("\\s+", "").isEmpty()) {
            return false;
        }

        // "No content", "Not available" gibi anlamsız içerikler
        String lowerContent = summary.toLowerCase();
        if (lowerContent.contains("no content") ||
                lowerContent.contains("not available") ||
                lowerContent.contains("content not found") ||
                lowerContent.equals("null") ||
                lowerContent.equals("n/a")) {
            return false;
        }

        return true;
    }
}