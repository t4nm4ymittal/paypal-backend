package com.paypal.reward_service.service.impl;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import com.paypal.reward_service.service.RewardService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RewardServiceImpl implements RewardService {

    private final RewardRepository rewardRepository;

    public RewardServiceImpl(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Override
    public Reward sendAward(Reward reward) {
        if (reward.getCreatedAt() == null) {
            reward.setCreatedAt(Instant.now());
        }
        return rewardRepository.save(reward);
    }

    @Override
    public Optional<Reward> getReward(Long id) {
        return rewardRepository.findById(id);
    }

    @Override
    public List<Reward> getRewardsByUser(Long userId) {
        return rewardRepository.findByRewardUserId(userId);
    }
}

