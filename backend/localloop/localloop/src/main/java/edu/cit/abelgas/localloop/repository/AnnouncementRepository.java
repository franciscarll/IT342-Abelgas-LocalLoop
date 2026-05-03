package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // ── Base paginated list — newest first ────────────────────────────────────
    Page<Announcement> findByBarangayOrderByCreatedAtDesc(String barangay, Pageable pageable);

    // ── Search by title keyword (case-insensitive) ────────────────────────────
    Page<Announcement> findByBarangayAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String barangay, String title, Pageable pageable);

    // ── Filter by category ────────────────────────────────────────────────────
    Page<Announcement> findByBarangayAndCategoryOrderByCreatedAtDesc(
            String barangay, String category, Pageable pageable);

    // ── Filter by category + title search ─────────────────────────────────────
    Page<Announcement> findByBarangayAndCategoryAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String barangay, String category, String title, Pageable pageable);

    // ── Pinned announcement for a barangay (latest pinned one) ────────────────
    Optional<Announcement> findFirstByBarangayAndIsPinnedTrueOrderByCreatedAtDesc(String barangay);

    // ── Browse by month: count per month/year ─────────────────────────────────
    @Query("""
            SELECT YEAR(a.createdAt) as yr, MONTH(a.createdAt) as mo, COUNT(a) as cnt
            FROM Announcement a
            WHERE a.barangay = :barangay
            GROUP BY YEAR(a.createdAt), MONTH(a.createdAt)
            ORDER BY yr DESC, mo DESC
            """)
    List<Object[]> countByMonth(@Param("barangay") String barangay);

    // ── Browse by category: count per category ────────────────────────────────
    @Query("""
            SELECT a.category, COUNT(a)
            FROM Announcement a
            WHERE a.barangay = :barangay
            GROUP BY a.category
            ORDER BY a.category ASC
            """)
    List<Object[]> countByCategory(@Param("barangay") String barangay);

    // ── Filter by month (for month-browse click) ──────────────────────────────
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.barangay = :barangay
              AND YEAR(a.createdAt)  = :year
              AND MONTH(a.createdAt) = :month
            ORDER BY a.createdAt DESC
            """)
    Page<Announcement> findByBarangayAndMonth(
            @Param("barangay") String barangay,
            @Param("year")     int year,
            @Param("month")    int month,
            Pageable pageable);


}