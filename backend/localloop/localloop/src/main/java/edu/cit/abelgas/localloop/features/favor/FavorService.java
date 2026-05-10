package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.request.FavorRequest;
import edu.cit.abelgas.localloop.dto.response.FavorResponse;
import edu.cit.abelgas.localloop.entity.Favor;
import edu.cit.abelgas.localloop.features.auth.User;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import edu.cit.abelgas.localloop.features.auth.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class FavorService {

    private final FavorRepository favorRepository;
    private final UserRepository  userRepository;

    public FavorService(FavorRepository favorRepository, UserRepository userRepository) {
        this.favorRepository = favorRepository;
        this.userRepository  = userRepository;
    }

    // ── Get favors in barangay (Dashboard + Favor Feed) ───────────────────────
    public Page<FavorResponse> getOpenFavors(String barangay, String category,
                                             String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String effectiveStatus = (status != null && !status.isBlank()) ? status : "OPEN";
        Page<Favor> results;

        boolean hasCategory = category != null && !category.isBlank()
                && !category.equalsIgnoreCase("All");

        if (hasCategory) {
            results = favorRepository.findByStatusAndBarangayAndCategory(
                    effectiveStatus, barangay, category, pageable);
        } else {
            results = favorRepository.findByStatusAndBarangay(
                    effectiveStatus, barangay, pageable);
        }
        return results.map(this::toResponse);
    }

    // ── My Activity ───────────────────────────────────────────────────────────
    public Page<FavorResponse> getMyPostedFavors(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return favorRepository.findByRequesterId(userId, pageable).map(this::toResponse);
    }

    public Page<FavorResponse> getMyClaimedFavors(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return favorRepository.findByClaimerId(userId, pageable).map(this::toResponse);
    }

    // ── Post a new favor ──────────────────────────────────────────────────────
    public FavorResponse postFavor(FavorRequest req, User requester) {
        Favor favor = Favor.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .status("OPEN")
                .barangay(requester.getBarangay())
                .requesterId(requester.getId())
                .requesterName(requester.getName())
                .dateNeeded(req.getDateNeeded())
                .build();
        return toResponse(favorRepository.save(favor));
    }

    // ── Edit a favor ──────────────────────────────────────────────────────────
    public FavorResponse updateFavor(Long favorId, FavorRequest req, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!favor.getRequesterId().equals(requester.getId()))
            throw new RuntimeException("Only the requester can edit this favor");
        if (!"OPEN".equals(favor.getStatus()))
            throw new RuntimeException("Only OPEN favors can be edited");

        favor.setTitle(req.getTitle());
        favor.setDescription(req.getDescription());
        favor.setCategory(req.getCategory());
        if (req.getDateNeeded() != null) favor.setDateNeeded(req.getDateNeeded());
        return toResponse(favorRepository.save(favor));
    }

    // ── Delete a favor ────────────────────────────────────────────────────────
    public void deleteFavor(Long favorId, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!favor.getRequesterId().equals(requester.getId()))
            throw new RuntimeException("Only the requester can delete this favor");
        if (!"OPEN".equals(favor.getStatus()))
            throw new RuntimeException("Only OPEN favors can be deleted");

        favorRepository.delete(favor);
    }

    // ── Claim a favor ─────────────────────────────────────────────────────────
    public FavorResponse claimFavor(Long favorId, User claimer) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!"OPEN".equals(favor.getStatus()))
            throw new RuntimeException("This favor is no longer available");
        if (favor.getRequesterId().equals(claimer.getId()))
            throw new RuntimeException("You cannot claim your own favor");

        favor.setStatus("CLAIMED");
        favor.setClaimerId(claimer.getId());
        favor.setClaimerName(claimer.getName());
        favor.setClaimedAt(LocalDateTime.now());
        return toResponse(favorRepository.save(favor));
    }

    // ── Cancel Claim (helper cancels) ─────────────────────────────────────────
    /**
     * Only the claimer can cancel.
     * Penalty: -1 reputation point (min 0).
     * Status resets to OPEN; claimer fields cleared.
     */
    public FavorResponse cancelClaim(Long favorId, User caller) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Favor not found"));

        if (!"CLAIMED".equals(favor.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only CLAIMED favors can have their claim cancelled");

        if (!caller.getId().equals(favor.getClaimerId()))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the claimer can cancel this claim");

        // ── Reputation penalty: -1 from helper ───────────────────────────────
        deductReputation(caller.getId(), 1);

        // ── Reset favor to OPEN ───────────────────────────────────────────────
        favor.setStatus("OPEN");
        favor.setClaimerId(null);
        favor.setClaimerName(null);
        favor.setClaimedAt(null);

        return toResponse(favorRepository.save(favor));
    }

    // ── Re-open Favor (requester re-opens abandoned favor) ────────────────────
    /**
     * Only the requester can re-open a CLAIMED favor.
     * Penalty: -2 reputation points from the helper who abandoned (min 0).
     * Status resets to OPEN; claimer fields cleared.
     */
    public FavorResponse reopenFavor(Long favorId, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Favor not found"));

        if (!"CLAIMED".equals(favor.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only CLAIMED favors can be re-opened");

        if (!requester.getId().equals(favor.getRequesterId()))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the requester can re-open this favor");

        // ── Reputation penalty: -2 from the helper who abandoned ─────────────
        if (favor.getClaimerId() != null) {
            deductReputation(favor.getClaimerId(), 2);
        }

        // ── Reset favor to OPEN ───────────────────────────────────────────────
        favor.setStatus("OPEN");
        favor.setClaimerId(null);
        favor.setClaimerName(null);
        favor.setClaimedAt(null);

        return toResponse(favorRepository.save(favor));
    }

    // ── Get a single favor by ID ──────────────────────────────────────────────
    public FavorResponse getFavorById(Long favorId, User requestingUser) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));
        return toResponse(favor);
    }

    // ── Confirm favor completion ──────────────────────────────────────────────
    public FavorResponse completeFavor(Long favorId, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!favor.getRequesterId().equals(requester.getId()))
            throw new RuntimeException("Only the requester can confirm completion");
        if (!"CLAIMED".equals(favor.getStatus()))
            throw new RuntimeException("This favor must be CLAIMED before it can be completed");

        favor.setStatus("COMPLETED");
        favor.setCompletedAt(LocalDateTime.now());
        favorRepository.save(favor);

        // +1 reputation to helper
        if (favor.getClaimerId() != null) {
            userRepository.findById(favor.getClaimerId()).ifPresent(helper -> {
                helper.setReputationScore(helper.getReputationScore() + 1);
                userRepository.save(helper);
            });
        }

        return toResponse(favor);
    }

    // ── Reputation helper ─────────────────────────────────────────────────────
    /**
     * Deducts points from a user's reputation score.
     * Score cannot go below 0.
     */
    private void deductReputation(Long userId, int points) {
        userRepository.findById(userId).ifPresent(user -> {
            int current = user.getReputationScore() != null ? user.getReputationScore() : 0;
            user.setReputationScore(Math.max(0, current - points));
            userRepository.save(user);
        });
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private FavorResponse toResponse(Favor f) {
        return FavorResponse.builder()
                .id(f.getId())
                .title(f.getTitle())
                .description(f.getDescription())
                .category(f.getCategory())
                .status(f.getStatus())
                .barangay(f.getBarangay())
                .requesterId(f.getRequesterId())
                .requesterName(f.getRequesterName())
                .claimerId(f.getClaimerId())
                .claimerName(f.getClaimerName())
                .dateNeeded(f.getDateNeeded())
                .createdAt(f.getCreatedAt())
                .completedAt(f.getCompletedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}