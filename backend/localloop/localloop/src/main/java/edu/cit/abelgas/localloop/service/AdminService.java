package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.response.AdminStatsResponse;
import edu.cit.abelgas.localloop.dto.response.RecentFavorResponse;
import edu.cit.abelgas.localloop.dto.response.ResidentResponse;
import edu.cit.abelgas.localloop.entity.Favor;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.AnnouncementRepository;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import edu.cit.abelgas.localloop.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository         userRepository;
    private final FavorRepository        favorRepository;
    private final AnnouncementRepository announcementRepository;

    public AdminService(UserRepository userRepository,
                        FavorRepository favorRepository,
                        AnnouncementRepository announcementRepository) {
        this.userRepository         = userRepository;
        this.favorRepository        = favorRepository;
        this.announcementRepository = announcementRepository;
    }

    // ── Dashboard stats ──────────────────────────────────────────────────────

    public AdminStatsResponse getStats(String barangay) {
        long residents     = userRepository.countResidentsByBarangay(barangay);
        long totalFavors   = favorRepository.countByBarangay(barangay);
        long announcements = announcementRepository.countByBarangay(barangay);
        long reputation    = userRepository.sumReputationByBarangay(barangay);
        long open          = favorRepository.countByBarangayAndStatus(barangay, "OPEN");
        long claimed       = favorRepository.countByBarangayAndStatus(barangay, "CLAIMED");
        long completed     = favorRepository.countByBarangayAndStatus(barangay, "COMPLETED");

        return AdminStatsResponse.builder()
                .totalResidents(residents)
                .totalFavors(totalFavors)
                .totalAnnouncements(announcements)
                .totalReputationGiven(reputation)
                .openFavors(open)
                .claimedFavors(claimed)
                .completedFavors(completed)
                .build();
    }

    // ── Recent favor activity (latest 5) ────────────────────────────────────

    public List<RecentFavorResponse> getRecentFavors(String barangay) {
        List<Favor> favors = favorRepository.findRecentByBarangay(
                barangay, PageRequest.of(0, 5));

        return favors.stream().map(f -> {
            String name = f.getRequesterName() != null ? f.getRequesterName() : "Unknown";
            return RecentFavorResponse.builder()
                    .id(f.getId())
                    .title(f.getTitle())
                    .requesterName(name)
                    .requesterInitials(initials(name))
                    .category(f.getCategory())
                    .status(f.getStatus())
                    .createdAt(f.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Residents page ───────────────────────────────────────────────────────

    /**
     * Returns paginated list of ALL users in the barangay (including admins,
     * active and inactive). Admin can see deactivated users to reactivate them.
     * searchBy: "name" | "email" | "both"
     */
    public Page<ResidentResponse> getResidents(String barangay,
                                               String search,
                                               String searchBy,
                                               int page,
                                               int size) {
        PageRequest pageable = PageRequest.of(page, size);
        boolean hasSearch = search != null && !search.isBlank();

        Page<User> users;
        if (!hasSearch) {
            users = userRepository.findAllByBarangay(barangay, pageable);
        } else {
            switch (searchBy == null ? "both" : searchBy.toLowerCase()) {
                case "name"  -> users = userRepository.findByBarangayAndName(barangay, search, pageable);
                case "email" -> users = userRepository.findByBarangayAndEmail(barangay, search, pageable);
                default      -> users = userRepository.findByBarangayAndNameOrEmail(barangay, search, pageable);
            }
        }

        return users.map(u -> {
            boolean isAdmin   = "ROLE_ADMIN".equals(u.getRole());
            long favorsPosted = isAdmin ? 0L : favorRepository.countByRequesterId(u.getId());
            return ResidentResponse.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .barangay(u.getBarangay())
                    .role(u.getRole())
                    .reputationScore(isAdmin ? null : u.getReputationScore())
                    .profileImageUrl(u.getProfileImageUrl())
                    .createdAt(u.getCreatedAt())
                    .favorsPosted(favorsPosted)
                    .active(u.isActive())
                    .build();
        });
    }

    public long getTotalAllUsers(String barangay) {
        return userRepository.countAllByBarangay(barangay);
    }

    // ── Deactivate resident ──────────────────────────────────────────────────

    /**
     * Soft-deactivates a resident. Admin cannot deactivate themselves.
     * Deactivated users are blocked in JwtAuthFilter and prompted to
     * select a new barangay on next login attempt.
     */
    public void deactivateResident(Long targetId, User admin) {
        if (admin.getId().equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot deactivate your own account.");
        }

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resident not found."));

        // Ensure target belongs to same barangay as admin
        if (!target.getBarangay().equals(admin.getBarangay())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only manage residents in your own barangay.");
        }

        if (!target.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Resident is already deactivated.");
        }

        target.setActive(false);
        userRepository.save(target);
    }

    // ── Reactivate resident ──────────────────────────────────────────────────

    /**
     * Reactivates a previously deactivated resident.
     * They will be able to log in again after this.
     */
    public void reactivateResident(Long targetId, User admin) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resident not found."));

        if (!target.getBarangay().equals(admin.getBarangay())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only manage residents in your own barangay.");
        }

        if (target.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Resident is already active.");
        }

        target.setActive(true);
        userRepository.save(target);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.length() > 2 ? sb.substring(0, 2) : sb.toString();
    }
}