package edu.cit.abelgas.localloop.dto.response;

public class AuthResponse {
    private UserResponse user;
    private String accessToken;

    public AuthResponse() {}

    public AuthResponse(UserResponse user, String accessToken) {
        this.user = user;
        this.accessToken = accessToken;
    }

    public UserResponse getUser() { return user; }
    public String getAccessToken() { return accessToken; }
    public void setUser(UserResponse user) { this.user = user; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UserResponse user;
        private String accessToken;

        public Builder user(UserResponse user) { this.user = user; return this; }
        public Builder accessToken(String token) { this.accessToken = token; return this; }

        public AuthResponse build() {
            return new AuthResponse(user, accessToken);
        }
    }
}