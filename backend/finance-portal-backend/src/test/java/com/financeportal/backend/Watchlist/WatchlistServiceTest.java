package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.quality.Strictness;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Watchlist Service Unit Tests")
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository priceRepository;

    @Mock
    private InstrumentMapper instrumentMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper cleanMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private WatchlistServiceImpl watchlistService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";

    private ForexInstrument testInstrument;
    private Watchlist testWatchlist;

    @BeforeEach
    void setUp() {
        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        testInstrument.setActive(true);

        testWatchlist = Watchlist.builder()
                .userId(TEST_USER_ID)
                .instrument(testInstrument)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Enstrüman watchlist'e başarıyla eklenmeli")
    void addToWatchlist_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(watchlistRepository.existsByUserIdAndInstrument(TEST_USER_ID, testInstrument)).thenReturn(false);
            when(watchlistRepository.save(any(Watchlist.class))).thenReturn(testWatchlist);
            when(redisTemplate.keys(anyString())).thenReturn(null);

            WatchlistDTO.WatchlistResponse result = watchlistService.addToWatchlist(1L);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Takip listesine eklendi");
            verify(watchlistRepository, times(1)).save(any(Watchlist.class));
        }
    }

    @Test
    @DisplayName("Zaten watchlist'te olan enstrüman tekrar eklenmemeli")
    void addToWatchlist_AlreadyExists_ReturnsFalse() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(watchlistRepository.existsByUserIdAndInstrument(TEST_USER_ID, testInstrument)).thenReturn(true);

            WatchlistDTO.WatchlistResponse result = watchlistService.addToWatchlist(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Bu enstrüman zaten takip listenizde");
            verify(watchlistRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Var olmayan enstrüman watchlist'e eklenemez")
    void addToWatchlist_InstrumentNotFound_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> watchlistService.addToWatchlist(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(watchlistRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Enstrüman watchlist'ten başarıyla çıkarılmalı")
    void removeFromWatchlist_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(watchlistRepository.existsByUserIdAndInstrument(TEST_USER_ID, testInstrument)).thenReturn(true);
            doNothing().when(watchlistRepository).deleteByUserIdAndInstrument(TEST_USER_ID, testInstrument);
            when(redisTemplate.keys(anyString())).thenReturn(null);

            WatchlistDTO.WatchlistResponse result = watchlistService.removeFromWatchlist(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Takip listesinden çıkarıldı");
            verify(watchlistRepository, times(1)).deleteByUserIdAndInstrument(TEST_USER_ID, testInstrument);
        }
    }

    @Test
    @DisplayName("Watchlist'te olmayan enstrüman çıkarılmaya çalışılırsa false dönmeli")
    void removeFromWatchlist_NotExists_ReturnsFalse() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(watchlistRepository.existsByUserIdAndInstrument(TEST_USER_ID, testInstrument)).thenReturn(false);

            WatchlistDTO.WatchlistResponse result = watchlistService.removeFromWatchlist(1L);

            assertThat(result.isSuccess()).isFalse();
            verify(watchlistRepository, never()).deleteByUserIdAndInstrument(any(), any());
        }
    }

    @Test
    @DisplayName("Enstrümanın watchlist'te olup olmadığı true dönmeli")
    void isInWatchlist_ReturnsTrue() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(watchlistRepository.existsByUserIdAndInstrument(TEST_USER_ID, testInstrument)).thenReturn(true);

            boolean result = watchlistService.isInWatchlist(1L);

            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("Enstrüman watchlist'te yoksa false dönmeli")
    void isInWatchlist_ReturnsFalse() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(watchlistRepository.existsByUserIdAndInstrument(TEST_USER_ID, testInstrument)).thenReturn(false);

            boolean result = watchlistService.isInWatchlist(1L);

            assertThat(result).isFalse();
        }
    }
}