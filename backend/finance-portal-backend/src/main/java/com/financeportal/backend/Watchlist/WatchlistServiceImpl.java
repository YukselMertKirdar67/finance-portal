package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.DTO.InstrumentResponseDTO;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Util.SecurityUtils;
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

    @Qualifier("objectMapper")
    private final ObjectMapper cleanMapper;

    private static final String CACHE_PREFIX = "watchlist:";
    private static final long CACHE_TTL = 5; // 5 dakika

    @Override
    @Transactional
    public WatchlistDTO.WatchlistResponse addToWatchlist(Long instrumentId) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();

        BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found: " + instrumentId
                ));

        if (watchlistRepository.existsByUserIdAndInstrument(currentUserId, instrument)) {
            return WatchlistDTO.WatchlistResponse.builder()
                    .success(false)
                    .message("Bu enstrüman zaten takip listenizde")
                    .build();
        }

        Watchlist watchlist = Watchlist.builder()
                .userId(currentUserId)
                .instrument(instrument)
                .build();

        watchlistRepository.save(watchlist);
        clearCache(currentUserId);

        log.info("✅ Added to watchlist: {} for user: {}", instrument.getSymbol(), currentUserId);

        return WatchlistDTO.WatchlistResponse.builder()
                .success(true)
                .message("Takip listesine eklendi")
                .build();
    }

    @Override
    @Transactional
    public WatchlistDTO.WatchlistResponse removeFromWatchlist(Long instrumentId) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();

        BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found: " + instrumentId
                ));

        if (!watchlistRepository.existsByUserIdAndInstrument(currentUserId, instrument)) {
            return WatchlistDTO.WatchlistResponse.builder()
                    .success(false)
                    .message("Bu enstrüman takip listenizde değil")
                    .build();
        }

        watchlistRepository.deleteByUserIdAndInstrument(currentUserId, instrument);
        clearCache(currentUserId);

        log.info("✅ Removed from watchlist: {} for user: {}", instrument.getSymbol(), currentUserId);

        return WatchlistDTO.WatchlistResponse.builder()
                .success(true)
                .message("Takip listesinden çıkarıldı")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistPageDTO getWatchlist(int page, int size) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        String cacheKey = CACHE_PREFIX + currentUserId + ":page:" + page + ":size:" + size;

        // ✅ JSON string olarak oku
        try {
            String cachedJson = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info("✅ Cache HIT - Watchlist for user: {}", currentUserId);
                return cleanMapper.readValue(cachedJson, WatchlistPageDTO.class);
            }
        } catch (Exception e) {
            log.warn("⚠️ Cache read error: {}", e.getMessage());
        }

        log.info("🔍 Cache MISS - Fetching from DB for user: {}", currentUserId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("addedAt").descending());
        Page<Watchlist> watchlistPage = watchlistRepository.findByUserId(currentUserId, pageable);

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
            log.info("✅ Cached watchlist (JSON) for user: {}", currentUserId);
        } catch (Exception e) {
            log.warn("⚠️ Cache write error: {}", e.getMessage());
        }

        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isInWatchlist(Long instrumentId) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        String cacheKey = CACHE_PREFIX + "check:" + currentUserId + ":inst:" + instrumentId;

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

        boolean exists = watchlistRepository.existsByUserIdAndInstrument(currentUserId, instrument);

        // ✅ String olarak kaydet
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(exists), CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("⚠️ Cache write error: {}", e.getMessage());
        }

        return exists;
    }

    private void clearCache(String userId) {
        try {
            String pattern = CACHE_PREFIX + userId + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Cleared {} cache keys for user: {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Cache clear error: {}", e.getMessage());
        }
    }
}