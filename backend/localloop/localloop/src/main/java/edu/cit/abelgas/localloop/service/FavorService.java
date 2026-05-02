package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.request.FavorRequest;
import edu.cit.abelgas.localloop.dto.response.FavorResponse;
import edu.cit.abelgas.localloop.entity.Favor;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import edu.cit.abelgas.localloop.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FavorService {

    private final FavorRepository favorRepository;
    private final UserRepository userRepository;

    public FavorService(FavorRepository favorRepository, UserRepository userRepository) {
        this.favorRepository = favorRepository;
        this.userRepository = userRepository;
    }

    // ── Get favors in barangay (Dashboard + Favor Feed) ───────────────────────
    public Page<FavorResponse> getOpenFavors(String barangay, String category,
                                             String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String effectiveStatus = (status != null && !status.isBlank()) ? status : "OPEN";
        Page<Favor> results;

        boolean hasCategory = category != null && !category.isBlank() && !category.equalsIgnoreCase("All");

        if (hasCategory) {
            results = favorRepository.findByStatusAndBarangayAndCategory(
                    effectiveStatus, barangay, category, pageable);
        } else {
            results = favorRepository.findByStatusAndBarangay(
                    effectiveStatus, barangay, pageable);
        }
        return results.map(this::toResponse);
    }

    // ── Get favors posted by the current user (My Activity → Posted tab) ──────
    public Page<FavorResponse> getMyPostedFavors(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return favorRepository.findByRequesterId(userId, pageable).map(this::toResponse);
    }

    // ── Get favors claimed by the current user (My Activity → Claimed tab) ────
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

    // ── Edit a favor (only requester, only OPEN status) ───────────────────────
    public FavorResponse updateFavor(Long favorId, FavorRequest req, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!favor.getRequesterId().equals(requester.getId())) {
            throw new RuntimeException("Only the requester can edit this favor");
        }
        if (!"OPEN".equals(favor.getStatus())) {
            throw new RuntimeException("Only OPEN favors can be edited");
        }

        favor.setTitle(req.getTitle());
        favor.setDescription(req.getDescription());
        favor.setCategory(req.getCategory());
        if (req.getDateNeeded() != null) {
            favor.setDateNeeded(req.getDateNeeded());
        }
        return toResponse(favorRepository.save(favor));
    }

    // ── Delete a favor (only requester, only OPEN status) ────────────────────
    public void deleteFavor(Long favorId, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!favor.getRequesterId().equals(requester.getId())) {
            throw new RuntimeException("Only the requester can delete this favor");
        }
        if (!"OPEN".equals(favor.getStatus())) {
            throw new RuntimeException("Only OPEN favors can be deleted");
        }

        favorRepository.delete(favor);
    }

    // ── Claim a favor ─────────────────────────────────────────────────────────
    public FavorResponse claimFavor(Long favorId, User claimer) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        if (!"OPEN".equals(favor.getStatus())) {
            throw new RuntimeException("This favor is no longer available");
        }
        if (favor.getRequesterId().equals(claimer.getId())) {
            throw new RuntimeException("You cannot claim your own favor");
        }

        favor.setStatus("CLAIMED");
        favor.setClaimerId(claimer.getId());
        favor.setClaimerName(claimer.getName());
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

        if (!favor.getRequesterId().equals(requester.getId())) {
            throw new RuntimeException("Only the requester can confirm completion");
        }
        if (!"CLAIMED".equals(favor.getStatus())) {
            throw new RuntimeException("This favor must be CLAIMED before it can be completed");
        }

        favor.setStatus("COMPLETED");
        favor.setCompletedAt(LocalDateTime.now());
        favorRepository.save(favor);

        if (favor.getClaimerId() != null) {
            userRepository.findById(favor.getClaimerId()).ifPresent(helper -> {
                helper.setReputationScore(helper.getReputationScore() + 1);
                userRepository.save(helper);
            });
        }

        return toResponse(favor);
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