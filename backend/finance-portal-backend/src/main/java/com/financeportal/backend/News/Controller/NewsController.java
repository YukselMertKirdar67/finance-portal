package com.financeportal.backend.News.Controller;

import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Service.NewsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/news")
@Tag(name = "News API", description = "Finans haberleri yönetimi")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    // 🔹 TÜM HABERLER (SAYFALI - REDIS UYUMLU)
    @Operation(summary = "Tüm haberleri sayfalı getir")
    @GetMapping
    public ResponseEntity<PageResponseDTO<NewsResponseDTO>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                newsService.getAllNews(page, size)
        );
    }

    // 🔹 KATEGORİYE GÖRE HABERLER (SAYFALI - REDIS UYUMLU)
    @Operation(summary = "Kategoriye göre haberleri sayfalı getir")
    @GetMapping("/category/{category}")
    public ResponseEntity<PageResponseDTO<NewsResponseDTO>> getNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            String decodedCategory =
                    java.net.URLDecoder.decode(category, java.nio.charset.StandardCharsets.UTF_8);

            return ResponseEntity.ok(
                    newsService.getNewsByCategory(decodedCategory, page, size)
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                    newsService.getNewsByCategory(category, page, size)
            );
        }
    }

    // 🔹 ID'YE GÖRE HABER
    @Operation(summary = "ID'ye göre haber getir")
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponseDTO> getNewsById(@PathVariable Long id) {
        return ResponseEntity.ok(
                newsService.getNewsById(id)
        );
    }

    // 🔹 YENİ HABER
    @Operation(summary = "Yeni haber ekle")
    @PostMapping
    public ResponseEntity<NewsResponseDTO> createNews(
            @Valid @RequestBody NewsRequestDTO requestDto
    ) {
        return new ResponseEntity<>(
                newsService.createNews(requestDto),
                HttpStatus.CREATED
        );
    }

    // 🔹 HABER GÜNCELLE
    @Operation(summary = "Haberi güncelle")
    @PutMapping("/{id}")
    public ResponseEntity<NewsResponseDTO> updateNews(
            @PathVariable Long id,
            @Valid @RequestBody NewsRequestDTO requestDto
    ) {
        return ResponseEntity.ok(
                newsService.updateNews(id, requestDto)
        );
    }

    // 🔹 HABER SİL
    @Operation(summary = "Haberi sil")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
