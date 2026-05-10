package edu.cit.abelgas.localloop.controller;

import edu.cit.abelgas.localloop.dto.request.FavorRequest;
import edu.cit.abelgas.localloop.dto.response.ApiResponse;
import edu.cit.abelgas.localloop.dto.response.FavorResponse;
import edu.cit.abelgas.localloop.features.auth.User;
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

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavorResponse>>> getOpenFavors(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "5")  int size,
            @RequestParam(required = false)    String category,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(ApiResponse.success(
                favorService.getOpenFavors(user.getBarangay(), category, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FavorResponse>> getFavorById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(favorService.getFavorById(id, user)));
    }

    @GetMapping("/my-posted")
    public ResponseEntity<ApiResponse<Page<FavorResponse>>> getMyPostedFavors(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                favorService.getMyPostedFavors(user.getId(), page, size)));
    }

    @GetMapping("/my-claimed")
    public ResponseEntity<ApiResponse<Page<FavorResponse>>> getMyClaimedFavors(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                favorService.getMyClaimedFavors(user.getId(), page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FavorResponse>> postFavor(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FavorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(favorService.postFavor(request, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FavorResponse>> updateFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FavorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                favorService.updateFavor(id, request, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        favorService.deleteFavor(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<FavorResponse>> claimFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(favorService.claimFavor(id, user)));
    }

    /**
     * PUT /api/favors/{id}/cancel-claim
     * Only the claimer can cancel. Deducts -1 rep from helper.
     */
    @PutMapping("/{id}/cancel-claim")
    public ResponseEntity<ApiResponse<FavorResponse>> cancelClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(favorService.cancelClaim(id, user)));
    }

    /**
     * PUT /api/favors/{id}/reopen
     * Only the requester can re-open a CLAIMED favor.
     * Deducts -2 rep from the helper who abandoned.
     */
    @PutMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<FavorResponse>> reopenFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(favorService.reopenFavor(id, user)));
    }

    /**
     * PUT /api/favors/{id}/complete
     * Only the requester can confirm completion. Awards +1 rep to helper.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<FavorResponse>> completeFavor(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(favorService.completeFavor(id, user)));
    }
}