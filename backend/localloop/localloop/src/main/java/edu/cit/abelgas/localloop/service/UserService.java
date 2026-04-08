package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.response.ReputationResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import org.springframework.stereotype.Service;
import edu.cit.abelgas.localloop.repository.UserRepository;

@Service
public class UserService {

    private final FavorRepository favorRepository;
    private final UserRepository userRepository;

    public UserService(FavorRepository favorRepository, UserRepository userRepository) {
        this.favorRepository = favorRepository;
        this.userRepository = userRepository;
    }

    public ReputationResponse getReputation(User user) {
        long posted    = favorRepository.countByRequesterId(user.getId());
        long completed = favorRepository.countByClaimerIdAndStatus(user.getId(), "COMPLETED");

        return ReputationResponse.builder()
                .reputationScore(user.getReputationScore())
                .favorsPosted(posted)
                .favorsCompleted(completed)
                .build();
    }

    // ── Get reputation stats for a user (used by Favor Detail sidebar) ───────
    public ReputationResponse getUserReputation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long posted    = favorRepository.countByRequesterId(userId);
        long completed = favorRepository.countByClaimerIdAndStatus(userId, "COMPLETED");

        return ReputationResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .reputationScore(user.getReputationScore())
                .favorsPosted(posted)
                .favorsCompleted(completed)
                .memberSince(user.getCreatedAt())
                .build();
    }
}