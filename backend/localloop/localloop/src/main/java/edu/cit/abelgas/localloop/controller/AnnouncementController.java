package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.response.AnnouncementResponse;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.AnnouncementService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * GET /api/announcements?page=0&size=3
     * Returns announcements for the authenticated user's barangay.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AnnouncementResponse>>> getAnnouncements(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {

        Page<AnnouncementResponse> data =
                announcementService.getAnnouncements(user.getBarangay(), page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}