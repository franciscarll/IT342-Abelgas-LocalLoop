package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.response.AdminStatsResponse;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.dto.response.RecentFavorResponse;
import edu.cit.abelgas.localloop.dto.response.ResidentResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats(
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getStats(user.getBarangay())));
    }

    @GetMapping("/favors/recent")
    public ResponseEntity<ApiResponse<List<RecentFavorResponse>>> getRecentFavors(
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getRecentFavors(user.getBarangay())));
    }

    // ── Residents ────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/residents?page=0&size=10&search=&searchBy=both
     * Returns ALL users in barangay (active + inactive, including admins).
     */
    @GetMapping("/residents")
    public ResponseEntity<ApiResponse<Page<ResidentResponse>>> getResidents(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "10")   int    size,
            @RequestParam(required = false)      String search,
            @RequestParam(defaultValue = "both") String searchBy) {
        requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getResidents(user.getBarangay(), search, searchBy, page, size)));
    }

    /**
     * GET /api/admin/residents/stats
     * Stat cards for the Residents page.
     */
    @GetMapping("/residents/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getResidentStats(
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        String barangay      = user.getBarangay();
        long totalUsers      = adminService.getTotalAllUsers(barangay);
        long totalReputation = adminService.getStats(barangay).getTotalReputationGiven();
        long totalCompleted  = adminService.getStats(barangay).getCompletedFavors();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalUsers",      totalUsers,
                "totalReputation", totalReputation,
                "totalCompleted",  totalCompleted
        )));
    }

    /**
     * PATCH /api/admin/residents/{id}/deactivate
     * Soft-deactivates a resident. Admin cannot deactivate themselves.
     */
    @PatchMapping("/residents/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateResident(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        adminService.deactivateResident(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * PATCH /api/admin/residents/{id}/reactivate
     * Reactivates a previously deactivated resident.
     */
    @PatchMapping("/residents/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateResident(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        adminService.reactivateResident(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Guard ────────────────────────────────────────────────────────────────

    private void requireAdmin(User user) {
        if (!"ROLE_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}