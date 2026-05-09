package com.financeportal.backend.WebSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPriceService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Tüm subscriber'lara fiyat güncellemesi gönderir.
     */
    public void sendPriceUpdate(PriceUpdateMessage message) {
        try {
            // Tüm fiyat güncellemeleri
            messagingTemplate.convertAndSend("/topic/prices", message);

            // Enstrümana özel kanal
            messagingTemplate.convertAndSend("/topic/prices/" + message.getInstrumentId(), message);

            log.debug("✅ WebSocket price update sent: {} = {}", message.getSymbol(), message.getCurrentPrice());
        } catch (Exception e) {
            log.error("❌ WebSocket send error: {}", e.getMessage());
        }
    }
}
