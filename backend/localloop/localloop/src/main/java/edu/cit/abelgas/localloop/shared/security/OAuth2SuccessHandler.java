package edu.cit.abelgas.localloop.shared.security;

import edu.cit.abelgas.localloop.features.auth.User;
import edu.cit.abelgas.localloop.features.auth.UserRepository;
import edu.cit.abelgas.localloop.shared.email.EmailService;
import edu.cit.abelgas.localloop.shared.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil        jwtUtil;
    private final EmailService emailService;
    // inside the class:
    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    public OAuth2SuccessHandler(UserRepository userRepository,
                                JwtUtil jwtUtil,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtUtil        = jwtUtil;
        this.emailService   = emailService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            // ── Reuse existing account — never create duplicates ───────────────
            user = existingUser.get();
        } else {
            user = User.builder()
                    .name(name)
                    .email(email)
                    .password(null)
                    .barangay("Not set")
                    .role("ROLE_USER")
                    .reputationScore(0)
                    .build();
            userRepository.save(user);

            try {
                emailService.sendWelcomeEmail(user.getName(), user.getEmail());
            } catch (Exception e) {
                log.warn("OAuth welcome email failed for {}: {}", user.getEmail(), e.getMessage());
            }
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        boolean hasPassword = (user.getPassword() != null);

        String redirectUrl = "http://localhost:3000/oauth2/callback"
                + "?token="           + token
                + "&id="              + user.getId()
                + "&name="            + URLEncoder.encode(user.getName(),     StandardCharsets.UTF_8)
                + "&email="           + URLEncoder.encode(user.getEmail(),    StandardCharsets.UTF_8)
                + "&barangay="        + URLEncoder.encode(user.getBarangay(), StandardCharsets.UTF_8)
                + "&role="            + user.getRole()
                + "&reputationScore=" + user.getReputationScore()
                + "&hasPassword="     + hasPassword;   // ← NEW

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}