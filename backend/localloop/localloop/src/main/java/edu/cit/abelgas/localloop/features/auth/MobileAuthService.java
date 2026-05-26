package edu.cit.abelgas.localloop.features.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import edu.cit.abelgas.localloop.shared.dto.UserResponse;
import edu.cit.abelgas.localloop.shared.email.EmailService;
import edu.cit.abelgas.localloop.shared.security.jwt.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class MobileAuthService {

    private static final Logger log = LoggerFactory.getLogger(MobileAuthService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public MobileAuthService(UserRepository userRepository,
                             JwtUtil jwtUtil,
                             EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtUtil        = jwtUtil;
        this.emailService   = emailService;
    }

    public AuthResponse googleSignIn(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                    .Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email      = payload.getEmail();
            String name       = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                if (pictureUrl != null && !pictureUrl.equals(user.getProfileImageUrl())) {
                    user.setProfileImageUrl(pictureUrl);
                    userRepository.save(user);
                }
            } else {
                user = User.builder()
                        .name(name)
                        .email(email)
                        .password(null)
                        .barangay("Not set")
                        .role("ROLE_USER")
                        .reputationScore(0)
                        .profileImageUrl(pictureUrl)
                        .build();
                userRepository.save(user);
                isNewUser = true;
            }

            if (isNewUser) {
                try {
                    emailService.sendWelcomeEmail(user.getName(), user.getEmail());
                } catch (Exception e) {
                    log.warn("Welcome email failed for mobile user {}: {}",
                            user.getEmail(), e.getMessage());
                }
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

            return AuthResponse.builder()
                    .user(mapToUserResponse(user))
                    .accessToken(token)
                    .build();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google Sign-In failed: " + e.getMessage(), e);
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .barangay(user.getBarangay())
                .role(user.getRole())
                .reputationScore(user.getReputationScore())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}