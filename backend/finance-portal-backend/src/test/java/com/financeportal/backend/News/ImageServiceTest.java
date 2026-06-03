package com.financeportal.backend.News;

import com.financeportal.backend.News.Service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService Unit Testleri")
class ImageServiceTest {

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService();
    }

    @Test
    @DisplayName("API'den gelen resim URL'i döndürülmeli")
    void getImageUrl_WithApiImage_ReturnsApiImage() {
        String result = imageService.getImageUrl("https://api.example.com/image.jpg", "FINANS");

        assertThat(result).isEqualTo("https://api.example.com/image.jpg");
    }

    @Test
    @DisplayName("API resmi yoksa FINANS kategori placeholder dönmeli")
    void getImageUrl_NoImage_FinansCategory_ReturnsPlaceholder() {
        String result = imageService.getImageUrl(null, "FINANS");

        assertThat(result).contains("unsplash.com");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("API resmi yoksa KRIPTO kategori placeholder dönmeli")
    void getImageUrl_NoImage_KriptoCategory_ReturnsPlaceholder() {
        String result = imageService.getImageUrl(null, "KRIPTO");

        assertThat(result).contains("unsplash.com");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("API resmi yoksa DOVIZ kategori placeholder dönmeli")
    void getImageUrl_NoImage_DovizCategory_ReturnsPlaceholder() {
        String result = imageService.getImageUrl(null, "DOVIZ");

        assertThat(result).contains("unsplash.com");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Bilinmeyen kategori için varsayılan placeholder dönmeli")
    void getImageUrl_NoImage_UnknownCategory_ReturnsDefault() {
        String result = imageService.getImageUrl(null, "BILINMEYEN");

        assertThat(result).contains("unsplash.com");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Boş API resmi için placeholder dönmeli")
    void getImageUrl_EmptyApiImage_ReturnsPlaceholder() {
        String result = imageService.getImageUrl("", "FINANS");

        assertThat(result).contains("unsplash.com");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Farklı kategoriler farklı placeholder URL döndürmeli")
    void getImageUrl_DifferentCategories_ReturnsDifferentUrls() {
        String finans = imageService.getImageUrl(null, "FINANS");
        String kripto = imageService.getImageUrl(null, "KRIPTO");
        String doviz = imageService.getImageUrl(null, "DOVIZ");

        assertThat(finans).isNotEqualTo(kripto);
        assertThat(finans).isNotEqualTo(doviz);
        assertThat(kripto).isNotEqualTo(doviz);
    }
}
