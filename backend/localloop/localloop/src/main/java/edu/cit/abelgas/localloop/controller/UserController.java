package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.dto.response.ReputationResponse;
import edu.cit.abelgas.localloop.dto.response.UserResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/me/reputation
     * Returns reputation score, favors posted, and favors completed
     * for the currently authenticated user.
     */
    @GetMapping("/me/reputation")
    public ResponseEntity<ApiResponse<ReputationResponse>> getMyReputation(
            @AuthenticationPrincipal User user) {
        ReputationResponse data = userService.getReputation(user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/users/{id}/reputation
     * Returns reputation stats for a specific user.
     * Used by the Favor Detail Page sidebar to display requester info.
     */
    @GetMapping("/{id}/reputation")
    public ResponseEntity<ApiResponse<ReputationResponse>> getUserReputation(
            @PathVariable Long id) {
        ReputationResponse data = userService.getUserReputation(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * PUT /api/profile
     * Updates the authenticated user's profile.
     * Used by SelectBarangayPage after Google OAuth login to save the barangay.
     * Accepts: { "barangay": "Quiot Pardo" }
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        UserResponse data = userService.updateProfile(user, body);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}