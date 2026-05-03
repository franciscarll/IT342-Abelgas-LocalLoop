package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.request.AnnouncementRequest;
import edu.cit.abelgas.localloop.dto.response.AnnouncementResponse;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * GET /api/announcements?page=0&size=4&search=&category=&year=&month=
     * Returns paginated announcements for the authenticated user's barangay.
     * Supports optional title search, category filter, and month/year filter.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AnnouncementResponse>>> getAnnouncements(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "4")  int size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String category,
            @RequestParam(required = false)    Integer year,
            @RequestParam(required = false)    Integer month) {

        Page<AnnouncementResponse> data = announcementService.getAnnouncements(
                user.getBarangay(), search, category, year, month, page, size);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/announcements/pinned
     * Returns the most recent pinned announcement for the resident sidebar.
     */
    @GetMapping("/pinned")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getPinned(
            @AuthenticationPrincipal User user) {

        return announcementService.getPinned(user.getBarangay())
                .map(a -> ResponseEntity.ok(ApiResponse.success(a)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    /**
     * GET /api/announcements/months
     * Returns [{year, month, count}] for the "Browse by Month" sidebar.
     */
    @GetMapping("/months")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthCounts(
            @AuthenticationPrincipal User user) {

        List<Map<String, Object>> data = announcementService.getMonthCounts(user.getBarangay());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/announcements/categories
     * Returns [{category, count}] for the "Browse by Category" sidebar.
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryCounts(
            @AuthenticationPrincipal User user) {

        List<Map<String, Object>> data = announcementService.getCategoryCounts(user.getBarangay());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * POST /api/announcements
     * Admin only: Create a new announcement.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnnouncementRequest request) {

        if (!"ROLE_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can create announcements");
        }
        AnnouncementResponse data = announcementService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    /**
     * PUT /api/announcements/{id}
     * Admin only: Update an existing announcement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnnouncementRequest request) {

        if (!"ROLE_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can edit announcements");
        }
        AnnouncementResponse data = announcementService.update(id, request, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * DELETE /api/announcements/{id}
     * Admin only: Delete an announcement.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        if (!"ROLE_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can delete announcements");
        }
        announcementService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}