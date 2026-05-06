package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.Favor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavorRepository extends JpaRepository<Favor, Long> {

    // ── Barangay-scoped queries (Dashboard + Favor Feed) ──────────────────────
    Page<Favor> findByStatusAndBarangay(String status, String barangay, Pageable pageable);
    Page<Favor> findByStatusAndBarangayAndCategory(String status, String barangay, String category, Pageable pageable);

    // ── My Activity queries ───────────────────────────────────────────────────
    // All favors posted by a specific user (any status)
    Page<Favor> findByRequesterId(Long requesterId, Pageable pageable);

    // All favors claimed by a specific user (any status)
    Page<Favor> findByClaimerId(Long claimerId, Pageable pageable);

    // Count all favors in a barangay (all statuses)
    @Query("SELECT COUNT(f) FROM Favor f WHERE f.barangay = :barangay")
    long countByBarangay(@Param("barangay") String barangay);

    // Count favors by barangay + status
    @Query("SELECT COUNT(f) FROM Favor f WHERE f.barangay = :barangay AND f.status = :status")
    long countByBarangayAndStatus(@Param("barangay") String barangay, @Param("status") String status);

    // Latest N favors in a barangay (all statuses), ordered newest first
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay ORDER BY f.createdAt DESC")
    List<Favor> findRecentByBarangay(@Param("barangay") String barangay, Pageable pageable);

    // ── Count queries (reputation + stats) ───────────────────────────────────
    long countByRequesterId(Long requesterId);
    long countByClaimerIdAndStatus(Long claimerId, String status);
}