package com.financeportal.backend.News;

import com.financeportal.backend.News.Controller.AdminNewsController;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Repository.NewsRepository;
import com.financeportal.backend.News.Service.ExternalNewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminNewsController.class)
@ActiveProfiles("test")
@DisplayName("AdminNewsController Unit Testleri")
class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalNewsService externalNewsService;

    @MockBean
    private NewsRepository newsRepository;

    @MockBean
    private CacheManager cacheManager;

    private News testNews;

    @BeforeEach
    void setUp() {
        testNews = new News();
        testNews.setId(1L);
        testNews.setTitle("Test Haber");
        testNews.setContent("Test içerik");
        testNews.setSource("Test Kaynak");
        testNews.setCategory("FINANS");
        testNews.setPublishDate(LocalDateTime.now());

        when(cacheManager.getCache(anyString())).thenReturn(null);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Haber çekme başarıyla tamamlanmalı")
    void fetchNewsFromApi_ReturnsOk() throws Exception {
        when(externalNewsService.fetchAndSaveFinanceNews()).thenReturn(Map.of(
                "totalFetched", 10,
                "totalSaved", 8,
                "totalSkipped", 2,
                "saved", Map.of("FINANS", 8),
                "skipped", Map.of("FINANS", 2)
        ));

        mockMvc.perform(post("/api/v1/admin/news/fetch")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm haberler başarıyla silinmeli")
    void deleteAllNews_ReturnsOk() throws Exception {
        when(newsRepository.count()).thenReturn(5L);
        doNothing().when(newsRepository).deleteAll();

        mockMvc.perform(delete("/api/v1/admin/news/all")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Haberler yenilenmeli")
    void refreshAllNews_ReturnsOk() throws Exception {
        when(newsRepository.count()).thenReturn(3L);
        doNothing().when(newsRepository).deleteAll();
        when(externalNewsService.fetchAndSaveFinanceNews()).thenReturn(Map.of(
                "totalFetched", 10,
                "totalSaved", 8,
                "totalSkipped", 2,
                "saved", Map.of("FINANS", 8),
                "skipped", Map.of("FINANS", 2)
        ));

        mockMvc.perform(post("/api/v1/admin/news/refresh")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kategoriye göre haberler silinmeli")
    void deleteNewsByCategory_ReturnsOk() throws Exception {
        when(newsRepository.findByCategory("FINANS")).thenReturn(List.of(testNews));
        doNothing().when(newsRepository).deleteAll(anyList());

        mockMvc.perform(delete("/api/v1/admin/news/category/FINANS")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kategoride haber yoksa bilgi mesajı dönmeli")
    void deleteNewsByCategory_NoNews_ReturnsOk() throws Exception {
        when(newsRepository.findByCategory("KRIPTO")).thenReturn(List.of());

        mockMvc.perform(delete("/api/v1/admin/news/category/KRIPTO")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm haberler getirilmeli")
    void getAllNewsSorted_ReturnsOk() throws Exception {
        when(newsRepository.findAllByOrderByPublishDateDesc()).thenReturn(List.of(testNews));

        mockMvc.perform(get("/api/v1/admin/news/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kategoriye göre haberler getirilmeli")
    void getNewsByCategorySorted_ReturnsOk() throws Exception {
        when(newsRepository.findByCategoryOrderByPublishDateDesc("FINANS"))
                .thenReturn(List.of(testNews));

        mockMvc.perform(get("/api/v1/admin/news/category/FINANS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.category").value("FINANS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kategoriler getirilmeli")
    void getAvailableCategories_ReturnsOk() throws Exception {
        when(newsRepository.findDistinctCategories()).thenReturn(List.of("FINANS", "KRIPTO"));
        when(newsRepository.findByCategory(anyString())).thenReturn(List.of(testNews));

        mockMvc.perform(get("/api/v1/admin/news/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Haber istatistikleri getirilmeli")
    void getNewsStats_ReturnsOk() throws Exception {
        when(newsRepository.count()).thenReturn(10L);
        when(newsRepository.findDistinctCategories()).thenReturn(List.of("FINANS"));
        when(newsRepository.findByCategory(anyString())).thenReturn(List.of(testNews));
        when(newsRepository.findAllByOrderByPublishDateDesc()).thenReturn(List.of(testNews));

        mockMvc.perform(get("/api/v1/admin/news/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getAllNewsSorted_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/news/all"))
                .andExpect(status().isUnauthorized());
    }
}
