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
                "FINANS", null, LocalDateTime.now());

        testPage = new PageResponseDTO<>(
                List.of(testNewsDTO), 0, 20, 1, 1, true);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm haberler başarıyla getirilmeli")
    void getAllNews_ReturnsOk() throws Exception {
        when(newsService.getAllNews(0, 20)).thenReturn(testPage);

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Haber"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kategoriye göre haberler getirilmeli")
    void getNewsByCategory_ReturnsOk() throws Exception {
        when(newsService.getNewsByCategory(eq("FINANS"), anyInt(), anyInt()))
                .thenReturn(testPage);

        mockMvc.perform(get("/api/v1/news/category/FINANS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("FINANS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ID ile haber getirilmeli")
    void getNewsById_ReturnsOk() throws Exception {
        when(newsService.getNewsById(1L)).thenReturn(testNewsDTO);

        mockMvc.perform(get("/api/v1/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Haber"));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getAllNews_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isUnauthorized());
    }
}
