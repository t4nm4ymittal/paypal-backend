package com.paypal.reward_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // id of the transaction that generated this reward
    private Long rewardTransactionId;

    // id of the user who receives the reward
    private Long rewardUserId;

    // points assigned (use transaction amount as integer points)
    private BigDecimal points;

    private Instant createdAt;

    public Reward() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRewardTransactionId() {
        return rewardTransactionId;
    }

    public void setRewardTransactionId(Long rewardTransactionId) {
        this.rewardTransactionId = rewardTransactionId;
    }

    public Long getRewardUserId() {
        return rewardUserId;
    }

    public void setRewardUserId(Long rewardUserId) {
        this.rewardUserId = rewardUserId;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

