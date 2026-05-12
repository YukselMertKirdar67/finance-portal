package com.financeportal.backend.News.Controller;

import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Repository.NewsRepository;
import com.financeportal.backend.News.Service.ExternalNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/news")
@Log4j2
@Tag(name = "Admin - Haber Yönetimi", description = "Admin haber yönetimi endpoint'leri")
public class AdminNewsController {

    private final ExternalNewsService externalNewsService;
    private final NewsRepository newsRepository;
    private final CacheManager cacheManager;

    public AdminNewsController(
            ExternalNewsService externalNewsService,
            NewsRepository newsRepository,
            CacheManager cacheManager
    ) {
        this.externalNewsService = externalNewsService;
        this.newsRepository = newsRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * NewsAPI'dan tüm kategorilerde haber çeker ve veritabanına kaydeder.
     * İşlem sonunda kategori bazlı istatistik döner.
     */
    @Operation(summary = "Haberleri API'den çek", description = "NewsAPI'dan tüm kategorilerde haber çeker ve veritabanına kaydeder")
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchNewsFromApi() {
        try {
            log.info("Starting news fetch from all categories...");

            Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

            int totalFetched = (int) result.get("totalFetched");
            int totalSaved = (int) result.get("totalSaved");
            int totalSkipped = (int) result.get("totalSkipped");

            // Yeni haberler eklendi, cache'i temizle
            clearAllCaches();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format(
                            "Toplam %d haber çekildi, %d tanesi kaydedildi, %d tanesi atlandı",
                            totalFetched, totalSaved, totalSkipped
                    ),
                    "stats", Map.of(
                            "fetched", totalFetched,
                            "saved", totalSaved,
                            "skipped", totalSkipped,
                            "successRate", totalFetched > 0 ?
                                    String.format("%.1f%%", (totalSaved * 100.0 / totalFetched)) : "0%"
                    ),
                    "breakdown", Map.of(
                            "saved", result.get("saved"),
                            "skipped", result.get("skipped")
                    ),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error in fetchNewsFromApi: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Haber çekme başarısız",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Veritabanındaki tüm haberleri siler ve cache'i temizler.
     */
    @Operation(summary = "Tüm haberleri sil", description = "Veritabanındaki tüm haberleri siler ve cache'i temizler")
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllNews() {
        try {
            long count = newsRepository.count();
            log.warn("Deleting all {} news from database...", count);

            newsRepository.deleteAll();

            // Cache'i temizle
            clearAllCaches();

            log.info("All news deleted successfully");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tüm haberler başarıyla silindi",
                    "deletedCount", count,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error deleting all news: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Haberleri silme başarısız",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Tüm haberleri silip NewsAPI'dan yeniden çeker.
     */
    @Operation(summary = "Haberleri yenile", description = "Tüm haberleri silip NewsAPI'dan yeniden çeker")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAllNews() {
        try {
            // 1. Önce tüm haberleri sil
            long deletedCount = newsRepository.count();
            log.warn("Deleting all {} news before refresh...", deletedCount);
            newsRepository.deleteAll();

            // Cache'i temizle
            clearAllCaches();

            log.info("All old news deleted");

            // 2. Yeni haberleri çek
            log.info("Fetching fresh news...");
            Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

            int totalFetched = (int) result.get("totalFetched");
            int totalSaved = (int) result.get("totalSaved");
            int totalSkipped = (int) result.get("totalSkipped");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format(
                            "%d eski haber silindi, %d yeni haber çekildi, %d tanesi kaydedildi",
                            deletedCount, totalFetched, totalSaved
                    ),
                    "deletedCount", deletedCount,
                    "stats", Map.of(
                            "fetched", totalFetched,
                            "saved", totalSaved,
                            "skipped", totalSkipped,
                            "successRate", totalFetched > 0 ?
                                    String.format("%.1f%%", (totalSaved * 100.0 / totalFetched)) : "0%"
                    ),
                    "breakdown", Map.of(
                            "saved", result.get("saved"),
                            "skipped", result.get("skipped")
                    ),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error refreshing news: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Haber yenileme başarısız",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Belirtilen kategorideki tüm haberleri siler.
     */
    @Operation(summary = "Kategoriye göre haberleri sil")
    @DeleteMapping("/category/{category}")
    public ResponseEntity<?> deleteNewsByCategory(@PathVariable String category) {
        try {
            // Repository'deki findByCategory metodunu kullan
            List<News> newsToDelete = newsRepository.findByCategory(category);
            int count = newsToDelete.size();

            if (count == 0) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", category + " kategorisinde silinecek haber yok",
                        "deletedCount", 0
                ));
            }

            newsRepository.deleteAll(newsToDelete);

            // Cache'i temizle
            clearAllCaches();

            log.info("Deleted {} news from category: {}", count, category);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", category + " kategorisindeki haberler silindi",
                    "deletedCount", count,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error deleting news by category: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Kategori haberleri silinemedi",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Tüm haberleri yayın tarihine göre sıralı getirir.
     */
    @Operation(summary = "Tüm haberleri getir", description = "Tüm haberleri yayın tarihine göre sıralı listeler")
    @GetMapping("/all")
    public ResponseEntity<?> getAllNewsSorted() {
        try {
            List<News> allNews = newsRepository.findAllByOrderByPublishDateDesc();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "news", allNews,
                    "total", allNews.size(),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error fetching all news: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Haberler getirilemedi",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Belirtilen kategorideki haberleri yayın tarihine göre sıralı getirir.
     */
    @Operation(summary = "Kategoriye göre haberleri getir", description = "Belirtilen kategorideki haberleri yayın tarihine göre sıralı listeler")
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getNewsByCategorySorted(@PathVariable String category) {
        try {
            List<News> news = newsRepository.findByCategoryOrderByPublishDateDesc(category);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "category", category,
                    "news", news,
                    "total", news.size(),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error fetching news by category: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Kategori haberleri getirilemedi",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Mevcut kategorileri ve her kategorideki haber sayısını getirir.
     */
    @Operation(summary = "Kategorileri getir", description = "Mevcut kategorileri ve her kategorideki haber sayısını listeler")
    @GetMapping("/categories")
    public ResponseEntity<?> getAvailableCategories() {
        try {
            List<String> categories = newsRepository.findDistinctCategories();

            // Her kategori için haber sayısını da ekle
            Map<String, Long> categoryCounts = new LinkedHashMap<>();
            for (String category : categories) {
                long count = newsRepository.findByCategory(category).size();
                categoryCounts.put(category, count);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "categories", categories,
                    "categoryCounts", categoryCounts,
                    "total", categories.size(),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error fetching categories: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Kategoriler getirilemedi",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Toplam haber sayısı, kategori dağılımı ve son güncelleme tarihini getirir.
     */
    @Operation(summary = "Haber istatistiklerini getir", description = "Toplam haber sayısı, kategori dağılımı ve son güncelleme tarihini döner")
    @GetMapping("/stats")
    public ResponseEntity<?> getNewsStats() {
        try {
            long totalNews = newsRepository.count();
            List<String> categories = newsRepository.findDistinctCategories();

            Map<String, Long> categoryCounts = new LinkedHashMap<>();
            for (String category : categories) {
                long count = newsRepository.findByCategory(category).size();
                categoryCounts.put(category, count);
            }

            // En son eklenen haber
            List<News> latestNews = newsRepository.findAllByOrderByPublishDateDesc();
            News lastNews = latestNews.isEmpty() ? null : latestNews.get(0);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", Map.of(
                            "totalNews", totalNews,
                            "totalCategories", categories.size(),
                            "categoryCounts", categoryCounts,
                            "lastUpdate", lastNews != null ? lastNews.getPublishDate() : null
                    ),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error fetching stats: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "İstatistikler getirilemedi",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * news, allNews ve newsByCategory cache'lerini temizler.
     */
    private void clearAllCaches() {
        try {
            Cache newsCache = cacheManager.getCache("news");
            if (newsCache != null) {
                newsCache.clear();
                log.info("Cache 'news' cleared");
            }

            Cache allNewsCache = cacheManager.getCache("allNews");
            if (allNewsCache != null) allNewsCache.clear();

            Cache newsByCategoryCache = cacheManager.getCache("newsByCategory");
            if (newsByCategoryCache != null) {
                newsByCategoryCache.clear();
                log.info("Cache 'newsByCategory' cleared");
            }
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
        }
    }
}
