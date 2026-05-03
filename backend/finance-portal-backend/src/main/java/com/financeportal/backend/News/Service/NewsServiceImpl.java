package com.financeportal.backend.News.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Mapper.NewsMapper;
import com.financeportal.backend.News.Repository.NewsRepository;
import com.financeportal.backend.News.Service.ExternalNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final ExternalNewsService externalNewsService;

    /**
     * Belirtilen kategoriye ait haberleri sayfalı olarak getirir.
     * Sonuçlar Redis cache'te tutulur.
     */
    @Override
    @Cacheable(
            value = "newsByCategory",
            key = "'cat:' + #category + ':page:' + #page + ':size:' + #size"
    )
    public PageResponseDTO<NewsResponseDTO> getNewsByCategory(
            String category, int page, int size) {
        log.info("Fetching news by category: {}, page: {}, size: {}", category, page, size);

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("publishDate"), Sort.Order.desc("id"))
        );

        Page<News> newsPage = newsRepository.findByCategoryIgnoreCase(category, pageable);

        List<NewsResponseDTO> content = newsPage.getContent().stream()
                .map(newsMapper::toResponseDto)
                .toList();

        log.info("✅ Fetched {} news for category: {}", content.size(), category);

        return new PageResponseDTO<>(
                content,
                newsPage.getNumber(),
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getTotalPages(),
                newsPage.isLast()
        );
    }

    /**
     * Tüm haberleri sayfalı olarak getirir.
     * Sonuçlar Redis cache'te tutulur.
     */
    @Override
    @Cacheable(
            value = "allNews",
            key = "'page:' + #page + ':size:' + #size"
    )
    public PageResponseDTO<NewsResponseDTO> getAllNews(int page, int size) {
        log.info("Fetching all news, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("publishDate"), Sort.Order.desc("id"))
        );

        Page<News> newsPage = newsRepository.findAll(pageable);

        List<NewsResponseDTO> content = newsPage.getContent().stream()
                .map(newsMapper::toResponseDto)
                .toList();

        log.info("✅ Fetched {} news total", content.size());

        return new PageResponseDTO<>(
                content,
                newsPage.getNumber(),
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getTotalPages(),
                newsPage.isLast()
        );
    }

    /**
     * ID'ye göre haber getirir.
     * Sonuç Redis cache'te tutulur.
     * Haber bulunamazsa ResourceNotFoundException fırlatır.
     */
    @Override
    @Cacheable(value = "news", key = "'by-id:' + #id")
    public NewsResponseDTO getNewsById(Long id) {
        log.info("Fetching news by ID: {}", id);

        News news = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("News not found with ID: {}", id);
                    return new ResourceNotFoundException("Haber bulunamadı. ID: " + id);
                });

        log.info("✅ News found: {}", news.getTitle());
        return newsMapper.toResponseDto(news);
    }

    /**
     * Veritabanındaki tüm haberleri siler ve cache'i temizler.
     * Silinen haber sayısını döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "news", allEntries = true),
            @CacheEvict(value = "allNews", allEntries = true),
            @CacheEvict(value = "newsByCategory", allEntries = true)
    })
    public long deleteAllNews() {
        long count = newsRepository.count();
        log.warn("Deleting all {} news from database...", count);
        newsRepository.deleteAll();
        log.info("✅ All {} news deleted successfully", count);
        return count;
    }

    /**
     * Belirtilen kategorideki tüm haberleri siler ve cache'i temizler.
     * Silinen haber sayısını döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "news", allEntries = true),
            @CacheEvict(value = "allNews", allEntries = true),
            @CacheEvict(value = "newsByCategory", allEntries = true)
    })
    public int deleteNewsByCategory(String category) {
        log.info("Deleting news by category: {}", category);
        List<News> newsToDelete = newsRepository.findByCategory(category);
        int count = newsToDelete.size();

        if (count == 0) {
            log.warn("No news found for category: {}", category);
            return 0;
        }

        newsRepository.deleteAll(newsToDelete);
        log.info("✅ Deleted {} news from category: {}", count, category);
        return count;
    }

    /**
     * Tüm haberleri silip NewsAPI'dan yeniden çeker.
     * Silinen ve eklenen haber sayılarını döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "news", allEntries = true),
            @CacheEvict(value = "allNews", allEntries = true),
            @CacheEvict(value = "newsByCategory", allEntries = true)
    })
    public Map<String, Object> refreshNews() {
        long deletedCount = newsRepository.count();
        log.warn("Deleting all {} news before refresh...", deletedCount);
        newsRepository.deleteAll();
        log.info("All old news deleted, fetching fresh news...");

        Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();
        log.info("✅ News refresh completed. Deleted: {}, Fetched: {}, Saved: {}",
                deletedCount, result.get("totalFetched"), result.get("totalSaved"));

        result.put("deletedCount", deletedCount);
        return result;
    }

    /**
     * Toplam haber sayısı, kategori dağılımı ve son güncelleme tarihini getirir.
     */
    @Override
    public Map<String, Object> getNewsStats() {
        log.info("Fetching news statistics");

        long totalNews = newsRepository.count();
        List<String> categories = newsRepository.findDistinctCategories();

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (String category : categories) {
            long count = newsRepository.findByCategory(category).size();
            categoryCounts.put(category, count);
        }

        List<News> latestNews = newsRepository.findAllByOrderByPublishDateDesc();
        News lastNews = latestNews.isEmpty() ? null : latestNews.get(0);

        log.info("✅ Stats fetched - Total: {}, Categories: {}", totalNews, categories.size());

        return Map.of(
                "totalNews", totalNews,
                "totalCategories", categories.size(),
                "categoryCounts", categoryCounts,
                "lastUpdate", lastNews != null ? lastNews.getPublishDate() : null
        );
    }
}