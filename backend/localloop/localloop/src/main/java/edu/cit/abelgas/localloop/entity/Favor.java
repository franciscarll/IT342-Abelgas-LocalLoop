package edu.cit.abelgas.localloop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "favors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category; // Errand, Pet Care, Tool Borrowing, Plant Watering, Other

    @Column(nullable = false)
    private String status; // OPEN, CLAIMED, COMPLETED, CANCELLED

    @Column(nullable = false)
    private String barangay;

    // ── Dates ──────────────────────────────────────────────────────────────
    @Column(name = "date_needed")
    private LocalDate dateNeeded;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Requester (who posted the favor) ───────────────────────────────────
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    // Denormalized for fast card rendering — avoids a JOIN on every favor list
    @Column(name = "requester_name")
    private String requesterName;

    // ── Claimer (who claimed the favor) ────────────────────────────────────
    @Column(name = "claimer_id")
    private Long claimerId;

    @Column(name = "claimer_name")
    private String claimerName;
}