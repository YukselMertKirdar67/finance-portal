package com.financeportal.backend.PriceAlert;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertServiceImpl implements PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository instrumentPriceRepository;
    private final NotificationService notificationService;

    /**
     * Kullanıcı için yeni fiyat alarmı oluşturur.
     * Alarm oluşturulduğunda aktif ve tetiklenmemiş olarak kaydedilir.
     */

    @Override
    @Transactional
    public PriceAlertDTO createAlert(CreatePriceAlertRequestDTO request) {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Creating price alert for user: {} instrument: {} condition: {} target: {}",
                userId, request.getInstrumentId(), request.getCondition(), request.getTargetPrice());

        BaseInstrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> {
                    log.error("Instrument not found: {}", request.getInstrumentId());
                    return new ResourceNotFoundException("Instrument not found");
                });

        PriceAlert alert = PriceAlert.builder()
                .userId(userId)
                .instrument(instrument)
                .targetPrice(request.getTargetPrice())
                .condition(request.getCondition())
                .active(true)
                .triggered(false)
                .build();

        PriceAlert saved = priceAlertRepository.save(alert);
        log.info("✅ Price alert created for user {}: {} {} {}",
                userId, instrument.getSymbol(), request.getCondition(), request.getTargetPrice());

        return toDTO(saved);
    }

    /**
     * Giriş yapmış kullanıcının tüm alarmlarını getirir (aktif + tetiklenmiş).
     */

    @Override
    @Transactional(readOnly = true)
    public List<PriceAlertDTO> getUserAlerts() {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Fetching all alerts for user: {}", userId);
        return priceAlertRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Giriş yapmış kullanıcının sadece aktif alarmlarını getirir.
     */

    @Override
    @Transactional(readOnly = true)
    public List<PriceAlertDTO> getActiveUserAlerts() {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Fetching active alerts for user: {}", userId);
        return priceAlertRepository.findByUserIdAndActiveTrue(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Belirtilen alarmı siler. Sadece kendi alarmını silebilir.
     */

    @Override
    @Transactional
    public void deleteAlert(Long alertId) {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        priceAlertRepository.deleteByIdAndUserId(alertId, userId);
        log.info("Deleting price alert: {} for user: {}", alertId, userId);
        log.info("✅ Price alert deleted: {} for user: {}", alertId, userId);
    }

    /**
     * Her 15 dakikada bir aktif alarmları kontrol eder.
     * Fiyat koşulu sağlanırsa bildirim gönderir ve alarmı devre dışı bırakır.
     */

    @Override
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void checkAlerts() {
        log.info("Checking price alerts...");

        List<PriceAlert> activeAlerts = priceAlertRepository.findByActiveTrue();

        for (PriceAlert alert : activeAlerts) {
            try {
                BigDecimal currentPrice = instrumentPriceRepository
                        .findTopByInstrumentOrderByTimestampDesc(alert.getInstrument())
                        .map(InstrumentPrice::getCurrentPrice)
                        .orElse(null);

                if (currentPrice == null) continue;

                boolean triggered = false;

                if (alert.getCondition() == AlertCondition.ABOVE &&
                        currentPrice.compareTo(alert.getTargetPrice()) >= 0) {
                    triggered = true;
                } else if (alert.getCondition() == AlertCondition.BELOW &&
                        currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                    triggered = true;
                }

                if (triggered) {
                    // Bildirimi gönder
                    notificationService.notifyPriceAlert(
                            alert.getUserId(),
                            alert.getInstrument().getSymbol(),
                            currentPrice.doubleValue(),
                            alert.getInstrument().getCurrency()
                    );

                    // Alarmı devre dışı bırak
                    alert.setTriggered(true);
                    alert.setActive(false);
                    alert.setTriggeredAt(LocalDateTime.now());
                    priceAlertRepository.save(alert);

                    log.info("✅ Price alert triggered: {} for user: {}",
                            alert.getInstrument().getSymbol(), alert.getUserId());
                }

            } catch (Exception e) {
                log.error("Error checking alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    /**
     * PriceAlert entity'sini PriceAlertDTO'ya dönüştürür.
     */

    private PriceAlertDTO toDTO(PriceAlert alert) {
        return PriceAlertDTO.builder()
                .id(alert.getId())
                .instrumentSymbol(alert.getInstrument().getSymbol())
                .instrumentName(alert.getInstrument().getName())
                .targetPrice(alert.getTargetPrice())
                .condition(alert.getCondition())
                .active(alert.isActive())
                .triggered(alert.isTriggered())
                .triggeredAt(alert.getTriggeredAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
