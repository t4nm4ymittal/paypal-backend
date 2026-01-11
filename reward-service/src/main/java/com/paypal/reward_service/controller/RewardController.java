package com.paypal.reward_service.controller;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.service.RewardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = "${cors.allowed.origins:http://localhost:3000}")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @PostMapping
    public ResponseEntity<Reward> createReward(@RequestBody Reward reward) {
        Reward saved = rewardService.sendAward(reward);
        return ResponseEntity.created(URI.create("/api/rewards/" + saved.getId())).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Reward>> getAllRewards() {
        // Not implemented pagination - returns all
        return ResponseEntity.ok(rewardService.getRewardsByUser(null));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reward>> getRewardsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(rewardService.getRewardsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reward> getRewardById(@PathVariable Long id) {
        Optional<Reward> r = rewardService.getReward(id);
        return r.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

