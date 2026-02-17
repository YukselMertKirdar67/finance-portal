package com.financeportal.backend.News.Scheduler;

import com.financeportal.backend.News.Service.ExternalNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsScheduler {

    private final ExternalNewsService externalNewsService;

    /**
     * ✅ Her 6 saatte bir haberleri güncelle
     * 06:00, 12:00, 18:00, 00:00
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void fetchNewsScheduled() {
        log.info("⏰ [SCHEDULER] Haber güncelleme başladı: {}", LocalDateTime.now());

        try {
            Map<String, Object> result = externalNewsService.fetchAndSaveFinanceNews();

            int totalFetched = (int) result.get("totalFetched");
            int totalSaved   = (int) result.get("totalSaved");
            int totalSkipped = (int) result.get("totalSkipped");

            log.info("✅ [SCHEDULER] Haber güncelleme tamamlandı: {}", LocalDateTime.now());
            log.info("📊 [SCHEDULER] Çekilen: {}, Kaydedilen: {}, Atlanan: {}",
                    totalFetched, totalSaved, totalSkipped);

        } catch (Exception e) {
            log.error("❌ [SCHEDULER] Haber güncelleme hatası: {}", e.getMessage(), e);
        }
    }
}
