package edu.cit.abelgas.localloop.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavorResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String status;
    private String barangay;

    // Requester
    private Long requesterId;
    private String requesterName;

    // Claimer
    private Long claimerId;
    private String claimerName;

    // Dates
    private LocalDate dateNeeded;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
}