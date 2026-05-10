package edu.cit.abelgas.localloop.features.profile;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    // Core user fields
    private Long id;
    private String name;
    private String email;
    private String barangay;
    private String role;
    private String profileImageUrl;
    private LocalDateTime createdAt;

    // Reputation stats (assembled from favors)
    private Integer reputationScore;
    private long favorsPosted;
    private long favorsClaimed;
    private long favorsCompleted;

    // ── NEW: tells the frontend whether this user has a password set ──────────
    // false  → Google user who has never set a password → show "Set Password" UI
    // true   → regular user OR Google user who already set a password
    //          → show full "Change Password" form (requires current password)
    private boolean hasPassword;
}