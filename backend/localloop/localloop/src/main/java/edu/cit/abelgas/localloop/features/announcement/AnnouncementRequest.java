package edu.cit.abelgas.localloop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    // Event | Health | Reminder | General
    @NotBlank(message = "Category is required")
    private String category;

    // Whether this announcement appears pinned in the resident sidebar
    private Boolean isPinned = false;
}