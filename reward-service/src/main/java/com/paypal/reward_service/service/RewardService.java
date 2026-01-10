package com.paypal.reward_service.service;

import com.paypal.reward_service.entity.Reward;

import java.util.List;
import java.util.Optional;

public interface RewardService {

    Reward sendAward(Reward reward);

    Optional<Reward> getReward(Long id);

    List<Reward> getRewardsByUser(Long userId);
}

