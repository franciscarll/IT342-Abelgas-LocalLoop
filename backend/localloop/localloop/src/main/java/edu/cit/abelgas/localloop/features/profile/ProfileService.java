package edu.cit.abelgas.localloop.features.profile;

import edu.cit.abelgas.localloop.shared.dto.UserResponse;
import edu.cit.abelgas.localloop.features.auth.User;
import edu.cit.abelgas.localloop.shared.exception.BadRequestException;
import edu.cit.abelgas.localloop.features.favor.FavorRepository;
import edu.cit.abelgas.localloop.features.auth.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;

@Service
public class ProfileService {

    private final UserRepository  userRepository;
    private final FavorRepository favorRepository;
    private final PasswordEncoder passwordEncoder;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public ProfileService(UserRepository userRepository,
                          FavorRepository favorRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.favorRepository = favorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getProfile(User user) {
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long posted    = favorRepository.countByRequesterId(dbUser.getId());
        long claimed   = favorRepository.countByClaimerIdAndStatus(dbUser.getId(), "CLAIMED");
        long completed = favorRepository.countByClaimerIdAndStatus(dbUser.getId(), "COMPLETED");

        return ProfileResponse.builder()
                .id(dbUser.getId())
                .name(dbUser.getName())
                .email(dbUser.getEmail())
                .barangay(dbUser.getBarangay())
                .role(dbUser.getRole())
                .profileImageUrl(dbUser.getProfileImageUrl())
                .createdAt(dbUser.getCreatedAt())
                .reputationScore(dbUser.getReputationScore())
                .favorsPosted(posted)
                .favorsClaimed(claimed)
                .favorsCompleted(completed)
                .hasPassword(dbUser.getPassword() != null)
                .build();
    }

    public UserResponse updateProfile(User user, ProfileUpdateRequest req) {
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getName() != null && !req.getName().isBlank()) {
            dbUser.setName(req.getName().trim());
        }

        boolean hasNewPassword =
                req.getNewPassword() != null && !req.getNewPassword().isBlank();

        if (hasNewPassword) {
            if (req.getNewPassword().length() < 8) {
                throw new BadRequestException("New password must be at least 8 characters.");
            }
            if (!req.getNewPassword().equals(req.getConfirmPassword())) {
                throw new BadRequestException("Passwords do not match.");
            }

            boolean isGoogleUserWithNoPassword = (dbUser.getPassword() == null);

            if (isGoogleUserWithNoPassword) {
                dbUser.setPassword(passwordEncoder.encode(req.getNewPassword()));
            } else {
                if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                    throw new BadRequestException("Current password is required.");
                }
                if (!passwordEncoder.matches(req.getCurrentPassword(), dbUser.getPassword())) {
                    throw new BadRequestException("Current password is incorrect.");
                }
                dbUser.setPassword(passwordEncoder.encode(req.getNewPassword()));
            }
        }

        userRepository.save(dbUser);
        return toUserResponse(dbUser);
    }

    public UserResponse uploadPhoto(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size must be 5MB or less");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new RuntimeException("Only JPG and PNG images are allowed");
        }
        try {
            byte[] bytes   = file.getBytes();
            String base64  = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + contentType + ";base64," + base64;

            User dbUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            dbUser.setProfileImageUrl(dataUrl);
            userRepository.save(dbUser);
            return toUserResponse(dbUser);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image file");
        }
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .barangay(u.getBarangay())
                .role(u.getRole())
                .reputationScore(u.getReputationScore())
                .profileImageUrl(u.getProfileImageUrl())
                .hasPassword(u.getPassword() != null)
                .build();
    }
}