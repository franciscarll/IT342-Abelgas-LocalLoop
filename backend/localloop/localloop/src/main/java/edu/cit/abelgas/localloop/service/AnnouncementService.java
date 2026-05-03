package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.request.AnnouncementRequest;
import edu.cit.abelgas.localloop.dto.response.AnnouncementResponse;
import edu.cit.abelgas.localloop.entity.Announcement;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.AnnouncementRepository;
import edu.cit.abelgas.localloop.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    // ── GET paginated announcements (resident view) ───────────────────────────
    // Supports optional search (title) and optional category filter.
    // Also supports optional month/year filter for "Browse by Month" sidebar.
    public Page<AnnouncementResponse> getAnnouncements(
            String barangay,
            String search,
            String category,
            Integer year,
            Integer month,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Month-browse filter takes priority
        if (year != null && month != null) {
            return announcementRepository
                    .findByBarangayAndMonth(barangay, year, month, pageable)
                    .map(this::toResponse);
        }

        boolean hasSearch   = search   != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank() && !category.equalsIgnoreCase("All");

        if (hasCategory && hasSearch) {
            return announcementRepository
                    .findByBarangayAndCategoryAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                            barangay, category, search, pageable)
                    .map(this::toResponse);
        }
        if (hasCategory) {
            return announcementRepository
                    .findByBarangayAndCategoryOrderByCreatedAtDesc(barangay, category, pageable)
                    .map(this::toResponse);
        }
        if (hasSearch) {
            return announcementRepository
                    .findByBarangayAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                            barangay, search, pageable)
                    .map(this::toResponse);
        }
        return announcementRepository
                .findByBarangayOrderByCreatedAtDesc(barangay, pageable)
                .map(this::toResponse);
    }

    // ── GET pinned announcement for sidebar ───────────────────────────────────
    public Optional<AnnouncementResponse> getPinned(String barangay) {
        return announcementRepository
                .findFirstByBarangayAndIsPinnedTrueOrderByCreatedAtDesc(barangay)
                .map(this::toResponse);
    }

    // ── GET browse-by-month counts ────────────────────────────────────────────
    public List<Map<String, Object>> getMonthCounts(String barangay) {
        List<Object[]> rows = announcementRepository.countByMonth(barangay);
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year",  ((Number) row[0]).intValue());
            m.put("month", ((Number) row[1]).intValue());
            m.put("count", ((Number) row[2]).longValue());
            return m;
        }).toList();
    }

    // ── GET browse-by-category counts ─────────────────────────────────────────
    public List<Map<String, Object>> getCategoryCounts(String barangay) {
        List<Object[]> rows = announcementRepository.countByCategory(barangay);
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", row[0]);
            m.put("count",    ((Number) row[1]).longValue());
            return m;
        }).toList();
    }

    // ── CREATE (admin only — role check done in controller) ───────────────────
    public AnnouncementResponse create(AnnouncementRequest req, User admin) {
        Announcement a = Announcement.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .category(req.getCategory() != null ? req.getCategory() : "General")
                .isPinned(req.getIsPinned() != null ? req.getIsPinned() : false)
                .barangay(admin.getBarangay())
                .postedBy(admin.getId())
                .build();
        return toResponse(announcementRepository.save(a));
    }

    // ── UPDATE (admin only) ───────────────────────────────────────────────────
    public AnnouncementResponse update(Long id, AnnouncementRequest req, User admin) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        // Admins can only edit announcements in their own barangay
        if (!a.getBarangay().equals(admin.getBarangay())) {
            throw new RuntimeException("Not authorized to edit this announcement");
        }

        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        if (req.getCategory() != null) a.setCategory(req.getCategory());
        if (req.getIsPinned() != null) a.setIsPinned(req.getIsPinned());

        return toResponse(announcementRepository.save(a));
    }

    // ── DELETE (admin only) ───────────────────────────────────────────────────
    public void delete(Long id, User admin) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        if (!a.getBarangay().equals(admin.getBarangay())) {
            throw new RuntimeException("Not authorized to delete this announcement");
        }
        announcementRepository.delete(a);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private AnnouncementResponse toResponse(Announcement a) {
        String username = userRepository.findById(a.getPostedBy())
                .map(User::getName)
                .orElse("Admin");
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .barangay(a.getBarangay())
                .category(a.getCategory())
                .isPinned(a.getIsPinned())
                .postedBy(username)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}