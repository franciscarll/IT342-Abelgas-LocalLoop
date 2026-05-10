package edu.cit.abelgas.localloop.features.profile;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks every reputation change for a user.
 * Points can be positive (earned) or negative (penalty).
 *
 * Examples:
 *   +1  "Completed favor: Buy medicine"
 *   -1  "Cancelled claimed favor: Walk the dog"
 *   -2  "Favor re-opened by requester: Fix the shelf"
 */
@Entity
@Table(name = "reputation_history")
public class ReputationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Positive = gain, negative = deduction */
    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public ReputationHistory() {}

    public ReputationHistory(Long userId, Integer points, String reason) {
        this.userId = userId;
        this.points = points;
        this.reason = reason;
    }

    public Long          getId()        { return id; }
    public Long          getUserId()    { return userId; }
    public Integer       getPoints()    { return points; }
    public String        getReason()    { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id)               { this.id = id; }
    public void setUserId(Long userId)       { this.userId = userId; }
    public void setPoints(Integer points)    { this.points = points; }
    public void setReason(String reason)     { this.reason = reason; }
    public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }
}