package edu.cit.abelgas.localloop.security;

import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.UserRepository;
import edu.cit.abelgas.localloop.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil        jwtUtil;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil        = jwtUtil;
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
            // ── New Google user — password is null ────────────────────────────
            user = User.builder()
                    .name(name)
                    .email(email)
                    .password(null)          // Google users start with no password
                    .barangay("Not set")
                    .role("ROLE_USER")
                    .reputationScore(0)
                    .build();
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // ── KEY FIX: include hasPassword so the frontend knows immediately ─────
        // whether to show "Set Password" or "Change Password" on the Profile page.
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