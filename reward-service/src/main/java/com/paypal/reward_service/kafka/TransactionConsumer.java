package com.paypal.reward_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.entity.TransactionRef;
import com.paypal.reward_service.repository.RewardRepository;
import com.paypal.reward_service.service.RewardService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    private final RewardService rewardService;
    private final RewardRepository rewardRepository;
    private final ObjectMapper objectMapper;

    public TransactionConsumer(RewardService rewardService, RewardRepository rewardRepository, ObjectMapper objectMapper) {
        this.rewardService = rewardService;
        this.rewardRepository = rewardRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transactions", groupId = "reward-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, String> record) {
        String message = record.value();
        try {
            logger.info("Reward consumer received record: topic={} partition={} offset={} key={}", record.topic(), record.partition(), record.offset(), record.key());
            TransactionRef tx = objectMapper.readValue(message, TransactionRef.class);

            // Check if reward for this transaction already exists
            var existing = rewardRepository.findByRewardTransactionId(tx.getId());
            if (existing != null) {
                logger.info("Reward already exists for transaction {} -> rewardId={}", tx.getId(), existing.getId());
                return;
            }

            Reward reward = new Reward();
            reward.setRewardTransactionId(tx.getId());
            // choose rewarding user - here we reward senderAccountId by default
            reward.setRewardUserId(tx.getSenderAccountId());
            // points = transaction amount (as BigDecimal)
            BigDecimal pts = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            reward.setPoints(pts);

            rewardService.sendAward(reward);
            logger.info("Created reward for transaction {} user {} points={}", tx.getId(), reward.getRewardUserId(), reward.getPoints());

        } catch (Exception e) {
            logger.error("Failed to process transaction message for reward: {}", message, e);
        }
    }
}

