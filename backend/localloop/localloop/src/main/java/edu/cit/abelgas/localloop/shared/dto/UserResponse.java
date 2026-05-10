package edu.cit.abelgas.localloop.shared.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String barangay;
    private String role;
    private Integer reputationScore;
    private String profileImageUrl;

    // ── NEW: lets the frontend know if a password exists after profile update ─
    // Used to keep AuthContext in sync so ProfilePage re-renders correctly.
    private boolean hasPassword;

    public UserResponse() {}

    public UserResponse(Long id, String name, String email, String barangay,
                        String role, Integer reputationScore, String profileImageUrl,
                        boolean hasPassword) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.barangay = barangay;
        this.role = role;
        this.reputationScore = reputationScore;
        this.profileImageUrl = profileImageUrl;
        this.hasPassword = hasPassword;
    }

    // Getters
    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getEmail()             { return email; }
    public String getBarangay()          { return barangay; }
    public String getRole()              { return role; }
    public Integer getReputationScore()  { return reputationScore; }
    public String getProfileImageUrl()   { return profileImageUrl; }
    public boolean isHasPassword()       { return hasPassword; }

    // Setters
    public void setId(Long id)                          { this.id = id; }
    public void setName(String name)                    { this.name = name; }
    public void setEmail(String email)                  { this.email = email; }
    public void setBarangay(String barangay)            { this.barangay = barangay; }
    public void setRole(String role)                    { this.role = role; }
    public void setReputationScore(Integer r)           { this.reputationScore = r; }
    public void setProfileImageUrl(String url)          { this.profileImageUrl = url; }
    public void setHasPassword(boolean hasPassword)     { this.hasPassword = hasPassword; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String barangay;
        private String role;
        private Integer reputationScore;
        private String profileImageUrl;
        private boolean hasPassword;

        public Builder id(Long id)                         { this.id = id; return this; }
        public Builder name(String name)                   { this.name = name; return this; }
        public Builder email(String email)                 { this.email = email; return this; }
        public Builder barangay(String barangay)           { this.barangay = barangay; return this; }
        public Builder role(String role)                   { this.role = role; return this; }
        public Builder reputationScore(Integer r)          { this.reputationScore = r; return this; }
        public Builder profileImageUrl(String url)         { this.profileImageUrl = url; return this; }
        public Builder hasPassword(boolean hasPassword)    { this.hasPassword = hasPassword; return this; }

        public UserResponse build() {
            return new UserResponse(id, name, email, barangay, role,
                    reputationScore, profileImageUrl, hasPassword);
        }
    }
}