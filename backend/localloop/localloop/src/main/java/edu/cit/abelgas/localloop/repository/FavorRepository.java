package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.Favor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavorRepository extends JpaRepository<Favor, Long> {

    // ── Resident-facing ───────────────────────────────────────────────────────

    Page<Favor> findByStatusAndBarangay(String status, String barangay, Pageable pageable);

    Page<Favor> findByStatusAndBarangayAndCategory(String status, String barangay,
                                                   String category, Pageable pageable);

    // ── My Activity ───────────────────────────────────────────────────────────

    Page<Favor> findByRequesterId(Long requesterId, Pageable pageable);

    Page<Favor> findByClaimerId(Long claimerId, Pageable pageable);

    // ── Count queries ─────────────────────────────────────────────────────────

    long countByRequesterId(Long requesterId);

    long countByClaimerIdAndStatus(Long claimerId, String status);

    @Query("SELECT COUNT(f) FROM Favor f WHERE f.barangay = :barangay")
    long countByBarangay(@Param("barangay") String barangay);

    @Query("SELECT COUNT(f) FROM Favor f WHERE f.barangay = :barangay AND f.status = :status")
    long countByBarangayAndStatus(@Param("barangay") String barangay,
                                  @Param("status") String status);

    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay ORDER BY f.createdAt DESC")
    List<Favor> findRecentByBarangay(@Param("barangay") String barangay, Pageable pageable);

    // ── Admin Favor Overview — explicit separate queries ──────────────────────
    // Avoids the Hibernate IS NULL binding bug by branching in AdminService.

    // 1. No filters
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay")
    Page<Favor> findAllByBarangay(@Param("barangay") String barangay, Pageable pageable);

    // 2. Search only (title OR requester name)
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND (LOWER(f.title) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(f.requesterName) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<Favor> findByBarangayAndSearch(@Param("barangay") String barangay,
                                        @Param("s") String search,
                                        Pageable pageable);

    // 3. Status only
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND f.status = :status")
    Page<Favor> findByBarangayAndStatus(@Param("barangay") String barangay,
                                        @Param("status") String status,
                                        Pageable pageable);

    // 4. Category only
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND f.category = :category")
    Page<Favor> findByBarangayAndCategory(@Param("barangay") String barangay,
                                          @Param("category") String category,
                                          Pageable pageable);

    // 5. Search + Status
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND f.status = :status AND (LOWER(f.title) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(f.requesterName) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<Favor> findByBarangayAndSearchAndStatus(@Param("barangay") String barangay,
                                                 @Param("s") String search,
                                                 @Param("status") String status,
                                                 Pageable pageable);

    // 6. Search + Category
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND f.category = :category AND (LOWER(f.title) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(f.requesterName) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<Favor> findByBarangayAndSearchAndCategory(@Param("barangay") String barangay,
                                                   @Param("s") String search,
                                                   @Param("category") String category,
                                                   Pageable pageable);

    // 7. Status + Category
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND f.status = :status AND f.category = :category")
    Page<Favor> findByBarangayAndStatusAndCategory(@Param("barangay") String barangay,
                                                   @Param("status") String status,
                                                   @Param("category") String category,
                                                   Pageable pageable);

    // 8. Search + Status + Category (all filters)
    @Query("SELECT f FROM Favor f WHERE f.barangay = :barangay AND f.status = :status AND f.category = :category AND (LOWER(f.title) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(f.requesterName) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<Favor> findByBarangayAndSearchAndStatusAndCategory(@Param("barangay") String barangay,
                                                            @Param("s") String search,
                                                            @Param("status") String status,
                                                            @Param("category") String category,
                                                            Pageable pageable);
}