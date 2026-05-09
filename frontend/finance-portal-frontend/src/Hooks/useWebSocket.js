import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://localhost:8080/ws';

export const useWebSocket = (onPriceUpdate) => {
    const clientRef = useRef(null);

    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('✅ WebSocket connected');

                // Tüm fiyat güncellemelerini dinle
                client.subscribe('/topic/prices', (message) => {
                    const priceUpdate = JSON.parse(message.body);
                    onPriceUpdate(priceUpdate);
                });
            },
            onDisconnect: () => {
                console.log('❌ WebSocket disconnected');
            },
            onStompError: (error) => {
                console.error('WebSocket error:', error);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, []);

    return clientRef;
};

export const useInstrumentWebSocket = (instrumentId, onPriceUpdate) => {
    const clientRef = useRef(null);

    useEffect(() => {
        if (!instrumentId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log(`✅ WebSocket connected for instrument: ${instrumentId}`);

                // Belirli enstrüman için dinle
                client.subscribe(`/topic/prices/${instrumentId}`, (message) => {
                    const priceUpdate = JSON.parse(message.body);
                    onPriceUpdate(priceUpdate);
                });
            },
            onDisconnect: () => {
                console.log('❌ WebSocket disconnected');
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [instrumentId]);

    return clientRef;
};