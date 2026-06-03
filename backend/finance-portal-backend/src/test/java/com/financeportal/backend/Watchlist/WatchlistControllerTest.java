package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatchlistController.class)
@ActiveProfiles("test")
@DisplayName("WatchlistController Testleri")
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchlistService watchlistService;

    @MockBean
    @org.springframework.beans.factory.annotation.Qualifier("objectMapper")
    private ObjectMapper cleanMapper;

    private WatchlistPageDTO testWatchlistPage;
    private WatchlistDTO.WatchlistResponse successResponse;

    @BeforeEach
    void setUp() {
        testWatchlistPage = WatchlistPageDTO.builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        successResponse = WatchlistDTO.WatchlistResponse.builder()
                .success(true)
                .message("İşlem başarılı")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Takip listesi başarıyla getirilmeli")
    void getWatchlist_ReturnsOk() throws Exception {
        when(watchlistService.getWatchlist(0, 20)).thenReturn(testWatchlistPage);
        when(cleanMapper.writeValueAsString(any())).thenReturn("{}");

        mockMvc.perform(get("/api/v1/watchlist"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Enstrüman takip listesine eklenmeli")
    void addToWatchlist_ReturnsOk() throws Exception {
        when(watchlistService.addToWatchlist(1L)).thenReturn(successResponse);

        mockMvc.perform(post("/api/v1/watchlist/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Enstrüman takip listesinden çıkarılmalı")
    void removeFromWatchlist_ReturnsOk() throws Exception {
        when(watchlistService.removeFromWatchlist(1L)).thenReturn(successResponse);

        mockMvc.perform(delete("/api/v1/watchlist/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Takip listesi kontrolü yapılmalı - true")
    void isInWatchlist_ReturnsTrue() throws Exception {
        when(watchlistService.isInWatchlist(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/watchlist/check/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getWatchlist_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist"))
                .andExpect(status().isUnauthorized());
    }
}