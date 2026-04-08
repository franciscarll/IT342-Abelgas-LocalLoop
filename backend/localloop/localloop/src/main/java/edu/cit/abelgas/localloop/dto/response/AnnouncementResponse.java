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
    private String postedBy;
    private LocalDateTime createdAt;
}