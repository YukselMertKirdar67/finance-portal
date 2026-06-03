package com.financeportal.backend.Home;

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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@ActiveProfiles("test")
@DisplayName("HomeController Testleri")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeService homeService;

    private HomePageDTO testHomePageDTO;

    @BeforeEach
    void setUp() {
        testHomePageDTO = HomePageDTO.builder()
                .marketOverview(List.of())
                .topGainers(List.of())
                .topLosers(List.of())
                .recentNews(List.of())
                .marketStats(HomePageDTO.MarketStats.builder()
                        .rising(5).falling(3).unchanged(2).build())
                .categories(List.of())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Anasayfa verileri başarıyla getirilmeli")
    void getHomePageData_ReturnsOk() throws Exception {
        when(homeService.getHomePageData()).thenReturn(testHomePageDTO);

        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketStats.rising").value(5))
                .andExpect(jsonPath("$.marketStats.falling").value(3));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getHomePageData_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isUnauthorized());
    }
}
