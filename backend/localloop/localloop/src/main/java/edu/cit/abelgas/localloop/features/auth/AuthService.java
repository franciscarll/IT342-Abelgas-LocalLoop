package edu.cit.abelgas.localloop.features.auth;

import edu.cit.abelgas.localloop.shared.dto.UserResponse;
import edu.cit.abelgas.localloop.shared.security.jwt.JwtUtil;
import edu.cit.abelgas.localloop.shared.exception.DuplicateEmailException;
import edu.cit.abelgas.localloop.shared.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .barangay(request.getBarangay())
                .role("ROLE_USER")
                .reputationScore(0)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .user(mapToUserResponse(user))
                .accessToken(token)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .user(mapToUserResponse(user))
                .accessToken(token)
                .build();
    }

    public UserResponse getMe(User user) {
        return mapToUserResponse(user);
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