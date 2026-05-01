package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Service.PortfolioHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioChangeNotificationScheduler {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingService holdingService;
    private final NotificationService notificationService;

    // Önceki değerleri hafızada tut
    private final Map<Long, BigDecimal> previousValues = new HashMap<>();

    // Her 1 saatte bir kontrol et
    @Scheduled(fixedRate = 3600000)
    public void checkPortfolioChanges() {
        log.info("Checking portfolio value changes...");

        List<Portfolio> allPortfolios = portfolioRepository.findAll();

        for (Portfolio portfolio : allPortfolios) {
            try {
                String currency = portfolio.getCurrency() != null ? portfolio.getCurrency() : "TRY";
                BigDecimal currentValue = holdingService.calculateCurrentValue(
                        portfolio.getId(), currency);

                if (currentValue.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal previousValue = previousValues.get(portfolio.getId());

                if (previousValue != null && previousValue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal changePercent = currentValue.subtract(previousValue)
                            .divide(previousValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    // %5'ten fazla değişimde bildirim gönder
                    if (changePercent.abs().compareTo(new BigDecimal("5")) >= 0) {
                        notificationService.notifyPortfolioChange(
                                portfolio.getUserId(),
                                portfolio.getName(),
                                changePercent.doubleValue(),
                                portfolio.getId()
                        );
                        log.info("Portfolio change notification sent for: {} ({}%)",
                                portfolio.getName(), changePercent);
                    }
                }

                previousValues.put(portfolio.getId(), currentValue);

            } catch (Exception e) {
                log.error("Error checking portfolio {}: {}", portfolio.getId(), e.getMessage());
            }
        }
    }
}
