package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.financeportal.backend.Instrument.DTO.InstrumentResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

public class WatchlistDTO{

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public static class WatchlistItemDTO implements Serializable {
        private Long id;
        private InstrumentResponseDTO instrument;
        private LocalDateTime addedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddToWatchlistRequest {
        private Long instrumentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchlistResponse {
        private boolean success;
        private String message;
        private Long watchlistId;
    }
}