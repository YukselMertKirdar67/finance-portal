package com.financeportal.backend.WebSocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketPriceService Unit Testleri")
class WebSocketPriceServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketPriceService webSocketPriceService;

    private PriceUpdateMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = PriceUpdateMessage.builder()
                .instrumentId(1L)
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .type("FOREX")
                .currentPrice(new BigDecimal("38.50"))
                .changeAmount(new BigDecimal("0.50"))
                .changePercent(new BigDecimal("1.32"))
                .previousClose(new BigDecimal("38.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Fiyat güncellemesi tüm kanal ve enstrümana özel kanala gönderilmeli")
    void sendPriceUpdate_SendsToBothTopics() {
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        webSocketPriceService.sendPriceUpdate(testMessage);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/prices"), eq(testMessage));
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/prices/1"), eq(testMessage));
    }

    @Test
    @DisplayName("Mesajlaşma template exception atsa da hata fırlatmamalı")
    void sendPriceUpdate_Exception_DoesNotThrow() {
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() ->
                webSocketPriceService.sendPriceUpdate(testMessage));
    }

    @Test
    @DisplayName("Farklı enstrüman ID'leri için farklı topic'lere gönderilmeli")
    void sendPriceUpdate_DifferentInstruments_SendsToDifferentTopics() {
        PriceUpdateMessage message2 = PriceUpdateMessage.builder()
                .instrumentId(2L)
                .symbol("EUR/TRY")
                .name("Euro")
                .type("FOREX")
                .currentPrice(new BigDecimal("42.00"))
                .changeAmount(BigDecimal.ZERO)
                .changePercent(BigDecimal.ZERO)
                .previousClose(new BigDecimal("42.00"))
                .timestamp(LocalDateTime.now())
                .build();

        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        webSocketPriceService.sendPriceUpdate(testMessage);
        webSocketPriceService.sendPriceUpdate(message2);

        verify(messagingTemplate).convertAndSend(eq("/topic/prices/1"), eq(testMessage));
        verify(messagingTemplate).convertAndSend(eq("/topic/prices/2"), eq(message2));
    }
}
