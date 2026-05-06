package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.response.AdminStatsResponse;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.dto.response.RecentFavorResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/admin/stats
     * Returns dashboard stat cards + favor status breakdown.
     * ADMIN only.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats(
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        AdminStatsResponse data = adminService.getStats(user.getBarangay());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/admin/favors/recent
     * Returns the 5 most recently posted favors (all statuses) in the barangay.
     * ADMIN only.
     */
    @GetMapping("/favors/recent")
    public ResponseEntity<ApiResponse<List<RecentFavorResponse>>> getRecentFavors(
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        List<RecentFavorResponse> data = adminService.getRecentFavors(user.getBarangay());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ── Guard helper ─────────────────────────────────────────────────────────

    private void requireAdmin(User user) {
        if (!"ROLE_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin access required");
        }
    }
}