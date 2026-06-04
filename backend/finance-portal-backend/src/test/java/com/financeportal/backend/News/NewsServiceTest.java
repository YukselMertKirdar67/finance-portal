package com.financeportal.backend.News;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Mapper.NewsMapper;
import com.financeportal.backend.News.Repository.NewsRepository;
import com.financeportal.backend.News.Service.ExternalNewsService;
import com.financeportal.backend.News.Service.NewsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("News Service Unit Tests")
class NewsServiceTest {

    @Mock private NewsRepository newsRepository;
    @Mock private NewsMapper newsMapper;
    @Mock private ExternalNewsService externalNewsService;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News testNews;
    private NewsResponseDTO testNewsDTO;

    @BeforeEach
    void setUp() {
        testNews = new News();
        testNews.setId(1L);
        testNews.setTitle("Test Haber Başlığı");
        testNews.setContent("Test haber içeriği");
        testNews.setSource("Test Kaynak");
        testNews.setCategory("FINANS");
        testNews.setPublishDate(LocalDateTime.now());

        testNewsDTO = new NewsResponseDTO(
                1L,
                "Test Haber Başlığı",
                "Test haber içeriği",
                "Test Kaynak",
                "FINANS",
                null,
                LocalDateTime.now(),
                null,
                null
        );
    }

    @Test
    @DisplayName("Tüm haberler Türkçe locale ile getirilmeli")
    void getAllNews_TurkishLocale_ReturnsPage() {
        Page<News> page = new PageImpl<>(List.of(testNews));
        when(newsRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(newsMapper.toResponseDto(testNews)).thenReturn(testNewsDTO);

        PageResponseDTO<NewsResponseDTO> result = newsService.getAllNews(0, 10, Locale.forLanguageTag("tr"));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Haber Başlığı");
    }

    @Test
    @DisplayName("Tüm haberler İngilizce locale ile getirilmeli")
    void getAllNews_EnglishLocale_ReturnsPage() {
        testNews.setTitleEn("Test News Title");
        testNews.setContentEn("Test news content");

        NewsResponseDTO englishDTO = new NewsResponseDTO(
                1L, "Test Haber Başlığı", "Test haber içeriği",
                "Test Kaynak", "FINANS", null, LocalDateTime.now(),null,null);
        englishDTO.setTitleEn("Test News Title");
        englishDTO.setContentEn("Test news content");

        Page<News> page = new PageImpl<>(List.of(testNews));
        when(newsRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(newsMapper.toResponseDto(testNews)).thenReturn(englishDTO);

        PageResponseDTO<NewsResponseDTO> result = newsService.getAllNews(0, 10, Locale.ENGLISH);

        assertThat(result).isNotNull();
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test News Title");
    }

    @Test
    @DisplayName("Kategoriye göre haberler Türkçe locale ile getirilmeli")
    void getNewsByCategory_ReturnsPage() {
        Page<News> page = new PageImpl<>(List.of(testNews));
        when(newsRepository.findByCategoryIgnoreCase(eq("FINANS"), any(Pageable.class)))
                .thenReturn(page);
        when(newsMapper.toResponseDto(testNews)).thenReturn(testNewsDTO);

        PageResponseDTO<NewsResponseDTO> result = newsService.getNewsByCategory(
                "FINANS", 0, 10, Locale.forLanguageTag("tr"));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo("FINANS");
    }

    @Test
    @DisplayName("ID ile haber Türkçe locale ile getirilmeli")
    void getNewsById_ReturnsNews() {
        when(newsRepository.findById(1L)).thenReturn(Optional.of(testNews));
        when(newsMapper.toResponseDto(testNews)).thenReturn(testNewsDTO);

        NewsResponseDTO result = newsService.getNewsById(1L, Locale.forLanguageTag("tr"));

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Haber Başlığı");
        assertThat(result.getSource()).isEqualTo("Test Kaynak");
    }

    @Test
    @DisplayName("Var olmayan haber için exception fırlatılmalı")
    void getNewsById_NotFound_ThrowsException() {
        when(newsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.getNewsById(999L, Locale.forLanguageTag("tr")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tüm haberler silinmeli")
    void deleteAllNews_Success() {
        when(newsRepository.count()).thenReturn(5L);
        doNothing().when(newsRepository).deleteAll();

        long result = newsService.deleteAllNews();

        assertThat(result).isEqualTo(5L);
        verify(newsRepository, times(1)).deleteAll();
    }

    @Test
    @DisplayName("Kategoriye göre haberler silinmeli")
    void deleteNewsByCategory_Success() {
        when(newsRepository.findByCategory("FINANS")).thenReturn(List.of(testNews));
        doNothing().when(newsRepository).deleteAll(anyList());

        int result = newsService.deleteNewsByCategory("FINANS");

        assertThat(result).isEqualTo(1);
        verify(newsRepository, times(1)).deleteAll(anyList());
    }

    @Test
    @DisplayName("Silinecek haber yoksa 0 dönmeli")
    void deleteNewsByCategory_NoNews_ReturnsZero() {
        when(newsRepository.findByCategory("KRIPTO")).thenReturn(List.of());

        int result = newsService.deleteNewsByCategory("KRIPTO");

        assertThat(result).isEqualTo(0);
        verify(newsRepository, never()).deleteAll(anyList());
    }
}