package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.request.FavorRequest;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.dto.response.FavorResponse;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.service.FavorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favors")
public class FavorController {

    private final FavorService favorService;

    public FavorController(FavorService favorService) {
        this.favorService = favorService;
    }

    /**
     * GET /api/favors?page=0&size=5&category=Errand
     * Returns open favors in the authenticated user's barangay.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavorResponse>>> getOpenFavors(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String category) {

        Page<FavorResponse> data = favorService.getOpenFavors(
                user.getBarangay(), category, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/favors/{id}
     * Returns details of a single favor by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FavorResponse>> getFavorById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        FavorResponse data = favorService.getFavorById(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * PUT /api/favors/{id}/complete
     * Confirms completion; only the requester can call this.
     * Awards +1 reputation to the helper (claimer).
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<FavorResponse>> completeFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        FavorResponse data = favorService.completeFavor(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * POST /api/favors
     * Create / post a new favor.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FavorResponse>> postFavor(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FavorRequest request) {

        FavorResponse data = favorService.postFavor(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data));
    }

    /**
     * POST /api/favors/{id}/claim
     * Claim an open favor.
     */
    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<FavorResponse>> claimFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        FavorResponse data = favorService.claimFavor(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}