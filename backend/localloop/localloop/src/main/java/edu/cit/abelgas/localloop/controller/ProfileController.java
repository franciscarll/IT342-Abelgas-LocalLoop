package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.request.ProfileUpdateRequest;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.dto.response.ProfileResponse;
import edu.cit.abelgas.localloop.dto.response.UserResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * GET /api/profile
     * Returns the full profile of the authenticated user,
     * including reputation stats and member since date.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        ProfileResponse data = profileService.getProfile(user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * PUT /api/profile
     * Updates name and optionally password.
     * Password is only updated if currentPassword + newPassword are provided.
     * Email and barangay cannot be changed.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse data = profileService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * POST /api/profile/upload
     * Accepts a JPG or PNG image (max 5MB).
     * Stores the image as a Base64 data URL in profile_image_url.
     * Returns the updated UserResponse so the frontend can sync AuthContext.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UserResponse>> uploadPhoto(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        UserResponse data = profileService.uploadPhoto(user, file);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}