package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.response.ReputationResponse;
import edu.cit.abelgas.localloop.dto.response.UserResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import edu.cit.abelgas.localloop.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    /**
     * Updates the authenticated user's profile fields.
     * Currently supports: barangay, name, profileImageUrl.
     * Called by SelectBarangayPage (PUT /api/users/profile) after Google OAuth.
     */
    public UserResponse updateProfile(User user, Map<String, String> body) {
        // Re-fetch from DB to get the latest persisted state
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("barangay") && body.get("barangay") != null
                && !body.get("barangay").isBlank()) {
            dbUser.setBarangay(body.get("barangay"));
        }
        if (body.containsKey("name") && body.get("name") != null
                && !body.get("name").isBlank()) {
            dbUser.setName(body.get("name"));
        }
        if (body.containsKey("profileImageUrl")) {
            dbUser.setProfileImageUrl(body.get("profileImageUrl"));
        }

        userRepository.save(dbUser);

        return UserResponse.builder()
                .id(dbUser.getId())
                .name(dbUser.getName())
                .email(dbUser.getEmail())
                .barangay(dbUser.getBarangay())
                .role(dbUser.getRole())
                .reputationScore(dbUser.getReputationScore())
                .profileImageUrl(dbUser.getProfileImageUrl())
                .build();
    }
}