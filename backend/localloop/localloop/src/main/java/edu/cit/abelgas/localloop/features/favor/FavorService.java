package edu.cit.abelgas.localloop.features.favor;

import edu.cit.abelgas.localloop.features.auth.User;
import edu.cit.abelgas.localloop.features.auth.UserRepository;
import edu.cit.abelgas.localloop.features.profile.ReputationHistory;
import edu.cit.abelgas.localloop.features.profile.ReputationHistoryRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class FavorService {

    private final FavorRepository             favorRepository;
    private final UserRepository              userRepository;
    private final ReputationHistoryRepository historyRepository;

    public FavorService(FavorRepository favorRepository,
                        UserRepository userRepository,
                        ReputationHistoryRepository historyRepository) {
        this.favorRepository   = favorRepository;
        this.userRepository    = userRepository;
        this.historyRepository = historyRepository;
    }

    // ── Get favors in barangay (Dashboard + Favor Feed) ───────────────────────
    public Page<FavorResponse> getOpenFavors(String barangay, String category,
                                             String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
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
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return favorRepository.findByRequesterId(userId, pageable).map(this::toResponse);
    }

    public Page<FavorResponse> getMyClaimedFavors(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
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

    // ── Cancel Claim ──────────────────────────────────────────────────────────
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

        deductReputation(caller.getId(), 1,
                "Cancelled claimed favor: " + favor.getTitle());

        favor.setStatus("OPEN");
        favor.setClaimerId(null);
        favor.setClaimerName(null);
        favor.setClaimedAt(null);

        return toResponse(favorRepository.save(favor));
    }

    // ── Re-open Favor ─────────────────────────────────────────────────────────
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

        if (favor.getClaimerId() != null) {
            deductReputation(favor.getClaimerId(), 2,
                    "Favor re-opened by requester: " + favor.getTitle());
        }

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

        if (favor.getClaimerId() != null) {
            awardReputation(favor.getClaimerId(), 1,
                    "Completed favor: " + favor.getTitle());
        }

        return toResponse(favor);
    }

    // ── Reputation helpers ────────────────────────────────────────────────────
    //
    // CRITICAL FIX: Previously used ifPresent() which silently swallowed any
    // exception thrown by historyRepository.save(). If the DB insert failed
    // for any reason (constraint, schema, type mismatch), the error was lost
    // and the method returned successfully — making it impossible to debug.
    //
    // Now using explicit findById + orElseThrow so ALL exceptions propagate
    // up the call stack and appear in the Spring Boot logs.

    private void awardReputation(Long userId, int points, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found for reputation award: " + userId));

        int current = user.getReputationScore() != null ? user.getReputationScore() : 0;
        user.setReputationScore(current + points);
        userRepository.save(user);

        // Save history — any failure here will now throw and appear in logs
        historyRepository.save(new ReputationHistory(userId, points, reason));
    }

    private void deductReputation(Long userId, int points, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found for reputation deduction: " + userId));

        int current = user.getReputationScore() != null ? user.getReputationScore() : 0;
        user.setReputationScore(Math.max(0, current - points));
        userRepository.save(user);

        // Save history — any failure here will now throw and appear in logs
        historyRepository.save(new ReputationHistory(userId, -points, reason));
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