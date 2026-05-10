package edu.cit.abelgas.localloop.dto.response;

import java.time.LocalDateTime;

public class ReputationHistoryResponse {

    private Integer       points;
    private String        reason;
    private LocalDateTime createdAt;

    public ReputationHistoryResponse() {}

    public ReputationHistoryResponse(Integer points, String reason, LocalDateTime createdAt) {
        this.points    = points;
        this.reason    = reason;
        this.createdAt = createdAt;
    }

    public Integer       getPoints()    { return points; }
    public String        getReason()    { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setPoints(Integer points)        { this.points = points; }
    public void setReason(String reason)         { this.reason = reason; }
    public void setCreatedAt(LocalDateTime time) { this.createdAt = time; }
}