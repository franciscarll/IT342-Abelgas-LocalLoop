package edu.cit.abelgas.localloop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String barangay;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "reputation_score", nullable = false)
    private Integer reputationScore;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = "ROLE_USER";
        if (reputationScore == null) reputationScore = 0;
    }

    public User() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getBarangay() { return barangay; }
    public String getRole() { return role; }
    public Integer getReputationScore() { return reputationScore; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public void setRole(String role) { this.role = role; }
    public void setReputationScore(Integer reputationScore) { this.reputationScore = reputationScore; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String password;
        private String barangay;
        private String role;
        private Integer reputationScore;
        private String profileImageUrl;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder barangay(String barangay) { this.barangay = barangay; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder reputationScore(Integer r) { this.reputationScore = r; return this; }
        public Builder profileImageUrl(String url) { this.profileImageUrl = url; return this; }
        public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }

        public User build() {
            User u = new User();
            u.id = this.id;
            u.name = this.name;
            u.email = this.email;
            u.password = this.password;
            u.barangay = this.barangay;
            u.role = this.role;
            u.reputationScore = this.reputationScore;
            u.profileImageUrl = this.profileImageUrl;
            u.createdAt = this.createdAt;
            return u;
        }
    }
}