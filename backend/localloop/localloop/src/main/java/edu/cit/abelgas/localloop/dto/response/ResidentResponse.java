package edu.cit.abelgas.localloop.dto.response;

import java.time.LocalDateTime;

public class ResidentResponse {

    private Long          id;
    private String        name;
    private String        email;
    private String        barangay;
    private String        role;
    private Integer       reputationScore;
    private String        profileImageUrl;
    private LocalDateTime createdAt;
    private long          favorsPosted;
    private boolean       active;

    public ResidentResponse() {}

    private ResidentResponse(Builder b) {
        this.id              = b.id;
        this.name            = b.name;
        this.email           = b.email;
        this.barangay        = b.barangay;
        this.role            = b.role;
        this.reputationScore = b.reputationScore;
        this.profileImageUrl = b.profileImageUrl;
        this.createdAt       = b.createdAt;
        this.favorsPosted    = b.favorsPosted;
        this.active          = b.active;
    }

    public Long          getId()              { return id; }
    public String        getName()            { return name; }
    public String        getEmail()           { return email; }
    public String        getBarangay()        { return barangay; }
    public String        getRole()            { return role; }
    public Integer       getReputationScore() { return reputationScore; }
    public String        getProfileImageUrl() { return profileImageUrl; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public long          getFavorsPosted()    { return favorsPosted; }
    public boolean       isActive()           { return active; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name, email, barangay, role, profileImageUrl;
        private Integer reputationScore;
        private LocalDateTime createdAt;
        private long favorsPosted;
        private boolean active = true;

        public Builder id(Long v)                { this.id = v;              return this; }
        public Builder name(String v)            { this.name = v;            return this; }
        public Builder email(String v)           { this.email = v;           return this; }
        public Builder barangay(String v)        { this.barangay = v;        return this; }
        public Builder role(String v)            { this.role = v;            return this; }
        public Builder reputationScore(Integer v){ this.reputationScore = v; return this; }
        public Builder profileImageUrl(String v) { this.profileImageUrl = v; return this; }
        public Builder createdAt(LocalDateTime v){ this.createdAt = v;       return this; }
        public Builder favorsPosted(long v)      { this.favorsPosted = v;    return this; }
        public Builder active(boolean v)         { this.active = v;          return this; }
        public ResidentResponse build()          { return new ResidentResponse(this); }
    }
}