package edu.cit.abelgas.localloop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FavorRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    // Optional — frontend can send this when the user picks a date
    private LocalDate dateNeeded;
}