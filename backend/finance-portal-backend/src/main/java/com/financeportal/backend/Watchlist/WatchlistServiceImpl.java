package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.DTO.InstrumentResponseDTO;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final InstrumentMapper instrumentMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Qualifier("objectMapper")  // ✅ EKLE - Temiz mapper
    private final ObjectMapper cleanMapper;

    private static final String MOCK_USER_ID = "mock-user-001";
    private static final String CACHE_PREFIX = "watchlist:";
    private static final long CACHE_TTL = 5; // 5 dakika

    @Override
    @Transactional
    public WatchlistDTO.WatchlistResponse addToWatchlist(Long instrumentId) {
        BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found: " + instrumentId
                ));

        if (watchlistRepository.existsByUserIdAndInstrument(MOCK_USER_ID, instrument)) {
            return WatchlistDTO.WatchlistResponse.builder()
                    .success(false)
                    .message("Bu enstrüman zaten takip listenizde")
                    .build();
        }

        Watchlist watchlist = Watchlist.builder()
                .userId(MOCK_USER_ID)
                .instrument(instrument)
                .build();

        watchlistRepository.save(watchlist);
        clearCache();

        log.info("✅ Added to watchlist: {}", instrument.getSymbol());

        return WatchlistDTO.WatchlistResponse.builder()
                .success(true)
                .message("Takip listesine eklendi")
                .build();
    }

    @Override
    @Transactional
    public WatchlistDTO.WatchlistResponse removeFromWatchlist(Long instrumentId) {
        BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found: " + instrumentId
                ));

        if (!watchlistRepository.existsByUserIdAndInstrument(MOCK_USER_ID, instrument)) {
            return WatchlistDTO.WatchlistResponse.builder()
                    .success(false)
                    .message("Bu enstrüman takip listenizde değil")
                    .build();
        }

        watchlistRepository.deleteByUserIdAndInstrument(MOCK_USER_ID, instrument);
        clearCache();

        log.info("✅ Removed from watchlist: {}", instrument.getSymbol());

        return WatchlistDTO.WatchlistResponse.builder()
                .success(true)
                .message("Takip listesinden çıkarıldı")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistPageDTO getWatchlist(int page, int size) {
        String cacheKey = CACHE_PREFIX + MOCK_USER_ID + ":page:" + page + ":size:" + size;

        // ✅ JSON string olarak oku
        try {
            String cachedJson = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info("✅ Cache HIT - Watchlist");
                return cleanMapper.readValue(cachedJson, WatchlistPageDTO.class);
            }
        } catch (Exception e) {
            log.warn("⚠️ Cache read error: {}", e.getMessage());
        }

        log.info("🔍 Cache MISS - Fetching from DB");

        Pageable pageable = PageRequest.of(page, size, Sort.by("addedAt").descending());
        Page<Watchlist> watchlistPage = watchlistRepository.findByUserId(MOCK_USER_ID, pageable);

        List<WatchlistDTO.WatchlistItemDTO> items = watchlistPage.getContent().stream()
                .map(item -> {
                    InstrumentPrice price = priceRepository
                            .findTopByInstrumentOrderByTimestampDesc(item.getInstrument())
                            .orElse(null);

                    InstrumentResponseDTO instrumentDTO = instrumentMapper
                            .toResponseDTO(item.getInstrument(), price);

                    return WatchlistDTO.WatchlistItemDTO.builder()
                            .id(item.getId())
                            .instrument(instrumentDTO)
                            .addedAt(item.getAddedAt())
                            .build();
                })
                .collect(Collectors.toList());

        WatchlistPageDTO result = WatchlistPageDTO.builder()
                .content(items)
                .pageNumber(watchlistPage.getNumber())
                .pageSize(watchlistPage.getSize())
                .totalElements(watchlistPage.getTotalElements())
                .totalPages(watchlistPage.getTotalPages())
                .first(watchlistPage.isFirst())
                .last(watchlistPage.isLast())
                .build();

        // ✅ JSON string olarak kaydet (@class YOK)
        try {
            String jsonToCache = cleanMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, CACHE_TTL, TimeUnit.MINUTES);
            log.info("✅ Cached watchlist (JSON)");
        } catch (Exception e) {
            log.warn("⚠️ Cache write error: {}", e.getMessage());
        }

        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isInWatchlist(Long instrumentId) {
        String cacheKey = CACHE_PREFIX + "check:" + MOCK_USER_ID + ":inst:" + instrumentId;

        // ✅ String olarak oku
        try {
            String cachedValue = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                return Boolean.parseBoolean(cachedValue);
            }
        } catch (Exception e) {
            log.warn("⚠️ Cache read error: {}", e.getMessage());
        }

        BaseInstrument instrument = instrumentRepository.findById(instrumentId).orElse(null);
        if (instrument == null) return false;

        boolean exists = watchlistRepository.existsByUserIdAndInstrument(MOCK_USER_ID, instrument);

        // ✅ String olarak kaydet
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(exists), CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("⚠️ Cache write error: {}", e.getMessage());
        }

        return exists;
    }

    private void clearCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Cleared {} cache keys", keys.size());
            }
        } catch (Exception e) {
            log.warn("⚠️ Cache clear error: {}", e.getMessage());
        }
    }
}