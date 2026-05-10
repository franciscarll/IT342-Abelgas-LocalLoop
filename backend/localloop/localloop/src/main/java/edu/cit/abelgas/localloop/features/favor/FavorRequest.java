package edu.cit.abelgas.localloop.features.favor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

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