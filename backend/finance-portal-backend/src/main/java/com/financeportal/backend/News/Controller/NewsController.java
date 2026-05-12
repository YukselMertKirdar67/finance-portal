package com.financeportal.backend.News.Controller;

import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@Log4j2
@Tag(name = "Haber işlemleri", description = "Kullanıcının yapabileceği işlemler")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Tüm haberleri sayfalı olarak getirir.
     */
    @GetMapping
    @Operation(summary = "Tüm haberleri sayfalı getir")
    public ResponseEntity<PageResponseDTO<NewsResponseDTO>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching all news, page: {}, size: {}", page, size);
        return ResponseEntity.ok(newsService.getAllNews(page, size));
    }

    /**
     * Kategoriye göre haberleri sayfalı olarak getirir.
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Kategoriye göre haberleri sayfalı getir")
    public ResponseEntity<PageResponseDTO<NewsResponseDTO>> getNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching news by category: {}, page: {}, size: {}", category, page, size);
        try {
            String decodedCategory =
                    java.net.URLDecoder.decode(category, java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(newsService.getNewsByCategory(decodedCategory, page, size));
        } catch (Exception e) {
            return ResponseEntity.ok(newsService.getNewsByCategory(category, page, size));
        }
    }

    /**
     * ID'ye göre haber getirir.
     */
    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre haber getir")
    public ResponseEntity<NewsResponseDTO> getNewsById(@PathVariable Long id) {
        log.info("Fetching news by ID: {}", id);
        return ResponseEntity.ok(newsService.getNewsById(id));
    }
}