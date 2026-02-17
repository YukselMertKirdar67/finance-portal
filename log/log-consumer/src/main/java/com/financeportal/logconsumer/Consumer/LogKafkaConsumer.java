package com.financeportal.logconsumer.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogKafkaConsumer {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${opensearch.index}")
    private String indexName;

    @KafkaListener(topics = "application-logs", groupId = "log-consumer-group")
    public void consume(String message) {

        if (message == null || message.trim().isEmpty()) {
            log.warn("Empty log message received from Kafka, skipping...");
            return;
        }

        try {
            Map<String, Object> logMap =
                    objectMapper.readValue(message, Map.class);

            IndexRequest request = new IndexRequest(indexName)
                    .source(logMap);

            client.index(request, RequestOptions.DEFAULT);

            log.info("Log indexed into OpenSearch");

        } catch (Exception e) {
            log.error("Failed to index log", e);
        }
    }
}