package com.paypal.transaction_service.kafka;

import com.paypal.transaction_service.transaction.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;


@Service
public class TransactionPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${transaction.kafka.topic:transactions}")
    private String topic;

    public TransactionPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Transaction tx) {
        try {
            String payload = objectMapper.writeValueAsString(tx);

            var future = kafkaTemplate.send(
                    topic,
                    tx.getId() != null ? tx.getId().toString() : null,
                    payload
            );



        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize transaction {}", tx.getId(), e);
        } catch (Exception e) {
            logger.error("Failed to publish transaction {}", tx.getId(), e);
        }
    }
}
