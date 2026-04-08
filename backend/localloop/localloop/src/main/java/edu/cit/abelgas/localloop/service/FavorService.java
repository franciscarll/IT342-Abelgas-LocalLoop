package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.request.FavorRequest;
import edu.cit.abelgas.localloop.dto.response.FavorResponse;
import edu.cit.abelgas.localloop.entity.Favor;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import edu.cit.abelgas.localloop.repository.UserRepository;
import java.time.LocalDateTime;

@Service
public class FavorService {

    private final FavorRepository favorRepository;
    private final UserRepository userRepository;
    public FavorService(FavorRepository favorRepository, UserRepository userRepository) {
        this.favorRepository = favorRepository;
        this.userRepository = userRepository;
    }

    // ── Get open favors near user (same barangay), optional category filter ──
    public Page<FavorResponse> getOpenFavors(String barangay, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Favor> results;
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) {
            results = favorRepository.findByStatusAndBarangayAndCategory("OPEN", barangay, category, pageable);
        } else {
            results = favorRepository.findByStatusAndBarangay("OPEN", barangay, pageable);
        }

        return results.map(this::toResponse);
    }

    // ── Post a new favor ─────────────────────────────────────────────────────
    public FavorResponse postFavor(FavorRequest req, User requester) {
        Favor favor = Favor.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .status("OPEN")
                .barangay(requester.getBarangay())
                .requesterId(requester.getId())
                .requesterName(requester.getName())   // denormalized for card display
                .dateNeeded(req.getDateNeeded())
                .build();

        return toResponse(favorRepository.save(favor));
    }

    // ── Claim a favor ────────────────────────────────────────────────────────
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

    // ── Get a single favor by ID ─────────────────────────────────────────────
    public FavorResponse getFavorById(Long favorId, User requestingUser) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));
        return toResponse(favor);
    }

    // ── Confirm favor completion ─────────────────────────────────────────────
    public FavorResponse completeFavor(Long favorId, User requester) {
        Favor favor = favorRepository.findById(favorId)
                .orElseThrow(() -> new RuntimeException("Favor not found"));

        // Only the requester can confirm completion
        if (!favor.getRequesterId().equals(requester.getId())) {
            throw new RuntimeException("Only the requester can confirm completion");
        }

        // Must be in CLAIMED state
        if (!"CLAIMED".equals(favor.getStatus())) {
            throw new RuntimeException("This favor must be CLAIMED before it can be completed");
        }

        // Update favor status
        favor.setStatus("COMPLETED");
        favor.setCompletedAt(LocalDateTime.now());
        favorRepository.save(favor);

        // Award +1 reputation to the claimer (helper)
        if (favor.getClaimerId() != null) {
            userRepository.findById(favor.getClaimerId()).ifPresent(helper -> {
                helper.setReputationScore(helper.getReputationScore() + 1);
                userRepository.save(helper);
            });
        }

        return toResponse(favor);
    }

    // ── Mapper ───────────────────────────────────────────────────────────────
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