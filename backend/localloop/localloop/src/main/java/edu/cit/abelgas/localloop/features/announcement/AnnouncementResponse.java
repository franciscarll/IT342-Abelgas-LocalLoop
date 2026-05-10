package edu.cit.abelgas.localloop.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private String barangay;
    private String category;       // ← NEW: Event | Health | Reminder | General
    private Boolean isPinned;      // ← NEW: drives the pinned sidebar card
    private String postedBy;       // resolved username (not raw ID)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}