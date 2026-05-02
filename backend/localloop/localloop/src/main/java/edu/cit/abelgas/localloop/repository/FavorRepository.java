package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.Favor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavorRepository extends JpaRepository<Favor, Long> {

    // ── Barangay-scoped queries (Dashboard + Favor Feed) ──────────────────────
    Page<Favor> findByStatusAndBarangay(String status, String barangay, Pageable pageable);
    Page<Favor> findByStatusAndBarangayAndCategory(String status, String barangay, String category, Pageable pageable);

    // ── My Activity queries ───────────────────────────────────────────────────
    // All favors posted by a specific user (any status)
    Page<Favor> findByRequesterId(Long requesterId, Pageable pageable);

    // All favors claimed by a specific user (any status)
    Page<Favor> findByClaimerId(Long claimerId, Pageable pageable);

    // ── Count queries (reputation + stats) ───────────────────────────────────
    long countByRequesterId(Long requesterId);
    long countByClaimerIdAndStatus(Long claimerId, String status);
}