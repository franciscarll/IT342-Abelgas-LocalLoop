package edu.cit.abelgas.localloop.features.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReputationResponse {

    private Long   userId;
    private String name;
    private int    reputationScore;
    private long   favorsPosted;
    private long   favorsCompleted;
    private LocalDateTime memberSince;

    /**
     * Full reputation history — both gains (+) and penalties (-).
     * Ordered newest first. Populated by GET /api/users/me/reputation.
     * Null for other users' reputation lookups (e.g. FavorDetail sidebar).
     */
    private List<ReputationHistoryResponse> history;
}