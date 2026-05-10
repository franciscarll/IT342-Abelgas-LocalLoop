package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.response.AdminStatsResponse;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.features.favor.FavorResponse;
import edu.cit.abelgas.localloop.features.favor.RecentFavorResponse;
import edu.cit.abelgas.localloop.dto.response.ResidentResponse;
import edu.cit.abelgas.localloop.features.auth.User;
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

    // ── Favor Overview ────────────────────────────────────────────────────────

    /**
     * GET /api/admin/favors
     *   ?page=0&size=10
     *   &search=       (title or requester name)
     *   &status=       (OPEN | CLAIMED | COMPLETED)
     *   &category=     (Errand | Pet Care | Tool Borrowing | Plant Watering | Other)
     *   &sort=         (newest [default] | oldest)
     *
     * Returns ALL favors in the admin's barangay across all statuses.
     * Note: /favors/recent must be declared BEFORE /favors to avoid
     * Spring mapping "recent" as a {id} path variable.
     */
    @GetMapping("/favors")
    public ResponseEntity<ApiResponse<Page<FavorResponse>>> getAdminFavors(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")      int    page,
            @RequestParam(defaultValue = "10")     int    size,
            @RequestParam(required = false)        String search,
            @RequestParam(required = false)        String status,
            @RequestParam(required = false)        String category,
            @RequestParam(defaultValue = "newest") String sort) {
        requireAdmin(user);
        Page<FavorResponse> data = adminService.getAdminFavors(
                user.getBarangay(), search, status, category, sort, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ── Residents ─────────────────────────────────────────────────────────────

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

    @PatchMapping("/residents/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateResident(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        requireAdmin(user);
        adminService.deactivateResident(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

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