package edu.cit.abelgas.localloop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO returned by GET /api/users/{id}/reputation
 * Used by FavorDetailPage to show requester stats in the sidebar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReputationResponse {

    private Long userId;
    private String name;
    private int reputationScore;
    private long favorsPosted;
    private long favorsCompleted;
    private LocalDateTime memberSince; // maps to user.createdAt
}