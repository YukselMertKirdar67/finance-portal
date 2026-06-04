package com.financeportal.backend.News;

import com.financeportal.backend.News.Controller.NewsController;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@ActiveProfiles("test")
@DisplayName("NewsController Testleri")
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    private NewsResponseDTO testNewsDTO;
    private PageResponseDTO<NewsResponseDTO> testPage;

    @BeforeEach
    void setUp() {
        testNewsDTO = new NewsResponseDTO(
                1L, "Test Haber", "Test içerik", "Test Kaynak",
                "FINANS", null, LocalDateTime.now(), null, null);

        testPage = new PageResponseDTO<>(
                List.of(testNewsDTO), 0, 20, 1, 1, true);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm haberler başarıyla getirilmeli")
    void getAllNews_ReturnsOk() throws Exception {
        when(newsService.getAllNews(anyInt(), anyInt(), any(Locale.class)))
                .thenReturn(testPage);

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Haber"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kategoriye göre haberler getirilmeli")
    void getNewsByCategory_ReturnsOk() throws Exception {
        when(newsService.getNewsByCategory(anyString(), anyInt(), anyInt(), any(Locale.class)))
                .thenReturn(testPage);

        mockMvc.perform(get("/api/v1/news/category/FINANS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("FINANS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ID ile haber getirilmeli")
    void getNewsById_ReturnsOk() throws Exception {
        when(newsService.getNewsById(eq(1L), any(Locale.class)))
                .thenReturn(testNewsDTO);

        mockMvc.perform(get("/api/v1/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Haber"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Accept-Language: en header ile İngilizce haber dönmeli")
    void getAllNews_EnglishLocale_ReturnsOk() throws Exception {
        NewsResponseDTO englishNews = new NewsResponseDTO(
                1L, "Test News", "Test content", "Test Source",
                "FINANCE", null, LocalDateTime.now(), null, null);
        PageResponseDTO<NewsResponseDTO> englishPage = new PageResponseDTO<>(
                List.of(englishNews), 0, 20, 1, 1, true);

        when(newsService.getAllNews(anyInt(), anyInt(), any(Locale.class)))
                .thenReturn(englishPage);

        mockMvc.perform(get("/api/v1/news")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test News"));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getAllNews_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isUnauthorized());
    }
}