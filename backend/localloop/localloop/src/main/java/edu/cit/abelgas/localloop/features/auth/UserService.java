package edu.cit.abelgas.localloop.features.auth;

import edu.cit.abelgas.localloop.features.profile.ReputationHistoryResponse;
import edu.cit.abelgas.localloop.features.profile.ReputationResponse;
import edu.cit.abelgas.localloop.shared.dto.UserResponse;
import edu.cit.abelgas.localloop.features.favor.FavorRepository;
import edu.cit.abelgas.localloop.features.profile.ReputationHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final FavorRepository             favorRepository;
    private final UserRepository              userRepository;
    private final ReputationHistoryRepository historyRepository;

    public UserService(FavorRepository favorRepository,
                       UserRepository userRepository,
                       ReputationHistoryRepository historyRepository) {
        this.favorRepository   = favorRepository;
        this.userRepository    = userRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * GET /api/users/me/reputation
     * Returns full reputation data including history for the current user.
     */
    public ReputationResponse getReputation(User user) {
        long posted    = favorRepository.countByRequesterId(user.getId());
        long completed = favorRepository.countByClaimerIdAndStatus(user.getId(), "COMPLETED");

        List<ReputationHistoryResponse> history = historyRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(h -> new ReputationHistoryResponse(h.getPoints(), h.getReason(), h.getCreatedAt()))
                .collect(Collectors.toList());

        return ReputationResponse.builder()
                .reputationScore(user.getReputationScore())
                .favorsPosted(posted)
                .favorsCompleted(completed)
                .history(history)
                .build();
    }

    /**
     * GET /api/users/{id}/reputation
     * Returns reputation stats for another user (Favor Detail sidebar).
     * History is intentionally excluded here — no need to expose it publicly.
     */
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
     * PUT /api/users/profile
     * Updates barangay, name, or profileImageUrl.
     * Called by SelectBarangayPage after Google OAuth.
     */
    public UserResponse updateProfile(User user, Map<String, String> body) {
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