package com.financeportal.backend.News;

import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Mapper.NewsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NewsMapper Unit Testleri")
class NewsMapperTest {

    private NewsMapper newsMapper;
    private News testNews;
    private NewsRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        newsMapper = Mappers.getMapper(NewsMapper.class);

        testNews = new News();
        testNews.setId(1L);
        testNews.setTitle("Test Haber Başlığı");
        testNews.setContent("Test haber içeriği");
        testNews.setSource("Bloomberg HT");
        testNews.setCategory("FINANS");
        testNews.setImageUrl("https://test-image.com/image.jpg");
        testNews.setPublishDate(LocalDateTime.of(2024, 1, 15, 10, 0));

        testRequestDTO = new NewsRequestDTO();
        ReflectionTestUtils.setField(testRequestDTO, "title", "Yeni Haber");
        ReflectionTestUtils.setField(testRequestDTO, "content", "Yeni haber içeriği");
        ReflectionTestUtils.setField(testRequestDTO, "source", "CNN Türk");
        ReflectionTestUtils.setField(testRequestDTO, "category", "KRIPTO");
        ReflectionTestUtils.setField(testRequestDTO, "imageUrl", "https://test-image.com/kripto.jpg");
    }

    @Test
    @DisplayName("News → NewsResponseDTO dönüşümü başarılı olmalı")
    void toResponseDto_Success() {
        NewsResponseDTO result = newsMapper.toResponseDto(testNews);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Haber Başlığı");
        assertThat(result.getContent()).isEqualTo("Test haber içeriği");
        assertThat(result.getSource()).isEqualTo("Bloomberg HT");
        assertThat(result.getCategory()).isEqualTo("FINANS");
        assertThat(result.getImageUrl()).isEqualTo("https://test-image.com/image.jpg");
        assertThat(result.getPublishDate()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
    }

    @Test
    @DisplayName("publishDate null olduğunda şimdiki zaman kullanılmalı")
    void toResponseDto_NullPublishDate_UsesCurrentTime() {
        testNews.setPublishDate(null);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        NewsResponseDTO result = newsMapper.toResponseDto(testNews);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(result.getPublishDate()).isBetween(before, after);
    }

    @Test
    @DisplayName("NewsRequestDTO → News entity dönüşümü başarılı olmalı")
    void toEntity_Success() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        News result = newsMapper.toEntity(testRequestDTO);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Yeni Haber");
        assertThat(result.getContent()).isEqualTo("Yeni haber içeriği");
        assertThat(result.getSource()).isEqualTo("CNN Türk");
        assertThat(result.getCategory()).isEqualTo("KRIPTO");
        assertThat(result.getImageUrl()).isEqualTo("https://test-image.com/kripto.jpg");
        assertThat(result.getPublishDate()).isBetween(before, after);
        assertThat(result.getId()).isNull();
    }

    @Test
    @DisplayName("toEntity - ID alanı set edilmemeli")
    void toEntity_IdNotSet() {
        News result = newsMapper.toEntity(testRequestDTO);

        assertThat(result.getId()).isNull();
    }

    @Test
    @DisplayName("Tüm alanlar doğru map edilmeli")
    void toResponseDto_AllFieldsMapped() {
        NewsResponseDTO result = newsMapper.toResponseDto(testNews);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isNotBlank();
        assertThat(result.getContent()).isNotBlank();
        assertThat(result.getSource()).isNotBlank();
        assertThat(result.getCategory()).isNotBlank();
        assertThat(result.getImageUrl()).isNotBlank();
        assertThat(result.getPublishDate()).isNotNull();
    }

    @Test
    @DisplayName("titleEn ve contentEn alanları map edilmeli")
    void toResponseDto_WithEnglishFields_Success() {
        testNews.setTitleEn("Test News Title");
        testNews.setContentEn("Test news content");

        NewsResponseDTO result = newsMapper.toResponseDto(testNews);

        assertThat(result.getTitleEn()).isEqualTo("Test News Title");
        assertThat(result.getContentEn()).isEqualTo("Test news content");
    }

    @Test
    @DisplayName("titleEn ve contentEn null olduğunda null dönmeli")
    void toResponseDto_NullEnglishFields_ReturnsNull() {
        testNews.setTitleEn(null);
        testNews.setContentEn(null);

        NewsResponseDTO result = newsMapper.toResponseDto(testNews);

        assertThat(result.getTitleEn()).isNull();
        assertThat(result.getContentEn()).isNull();
    }

    @Test
    @DisplayName("toEntity - titleEn ve contentEn set edilmemeli")
    void toEntity_EnglishFieldsIgnored() {
        News result = newsMapper.toEntity(testRequestDTO);

        assertThat(result.getTitleEn()).isNull();
        assertThat(result.getContentEn()).isNull();
    }
}
