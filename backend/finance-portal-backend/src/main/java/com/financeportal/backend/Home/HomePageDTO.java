package com.financeportal.backend.Home;

import com.financeportal.backend.News.DTO.NewsResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePageDTO {

    private List<MarketOverviewItem> marketOverview;
    private List<InstrumentSummary> topGainers;
    private List<InstrumentSummary> topLosers;
    private List<NewsResponseDTO> recentNews;
    private MarketStats marketStats;
    private List<CategorySummary> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketOverviewItem {
        private Long id;
        private String symbol;
        private String name;
        private String currentPrice;
        private String change;
        private String changePercent;
        private Boolean isPositive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentSummary {
        private Long id;
        private String symbol;
        private String name;
        private String currentPrice;
        private String change;
        private String changePercent;
        private Boolean isPositive;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketStats {
        private Integer rising;      // Yükseliş sayısı
        private Integer falling;     // Düşüş sayısı
        private Integer unchanged;   // Değişmedi
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String type;
        private String displayName;
        private Integer count;
        private String iconName;  // Frontend için icon adı
    }
}
