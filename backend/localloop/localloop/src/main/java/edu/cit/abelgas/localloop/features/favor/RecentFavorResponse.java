package edu.cit.abelgas.localloop.dto.response;

import java.time.LocalDateTime;

public class RecentFavorResponse {

    private Long   id;
    private String title;
    private String requesterName;
    private String requesterInitials;
    private String category;
    private String status;
    private LocalDateTime createdAt;

    public RecentFavorResponse() {}

    private RecentFavorResponse(Builder b) {
        this.id                = b.id;
        this.title             = b.title;
        this.requesterName     = b.requesterName;
        this.requesterInitials = b.requesterInitials;
        this.category          = b.category;
        this.status            = b.status;
        this.createdAt         = b.createdAt;
    }

    public Long   getId()                { return id; }
    public String getTitle()             { return title; }
    public String getRequesterName()     { return requesterName; }
    public String getRequesterInitials() { return requesterInitials; }
    public String getCategory()          { return category; }
    public String getStatus()            { return status; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String title, requesterName, requesterInitials, category, status;
        private LocalDateTime createdAt;

        public Builder id(Long v)                { this.id = v;                return this; }
        public Builder title(String v)           { this.title = v;             return this; }
        public Builder requesterName(String v)   { this.requesterName = v;     return this; }
        public Builder requesterInitials(String v){ this.requesterInitials = v; return this; }
        public Builder category(String v)        { this.category = v;          return this; }
        public Builder status(String v)          { this.status = v;            return this; }
        public Builder createdAt(LocalDateTime v){ this.createdAt = v;         return this; }
        public RecentFavorResponse build()       { return new RecentFavorResponse(this); }
    }
}