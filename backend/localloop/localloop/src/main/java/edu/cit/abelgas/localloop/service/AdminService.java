package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.response.AdminStatsResponse;
import edu.cit.abelgas.localloop.dto.response.RecentFavorResponse;
import edu.cit.abelgas.localloop.entity.Favor;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.AnnouncementRepository;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import edu.cit.abelgas.localloop.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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