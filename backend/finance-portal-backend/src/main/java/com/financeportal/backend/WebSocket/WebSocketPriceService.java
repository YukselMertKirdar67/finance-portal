package com.financeportal.backend.WebSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
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
