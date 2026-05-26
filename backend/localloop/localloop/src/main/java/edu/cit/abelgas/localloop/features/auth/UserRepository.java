package edu.cit.abelgas.localloop.features.auth;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // ── Admin stats ──────────────────────────────────────────────────────────

    @Query("SELECT COUNT(u) FROM User u WHERE u.barangay = :barangay AND u.role != 'ROLE_ADMIN'")
    long countResidentsByBarangay(@Param("barangay") String barangay);

    @Query("SELECT COALESCE(SUM(u.reputationScore), 0) FROM User u WHERE u.barangay = :barangay")
    long sumReputationByBarangay(@Param("barangay") String barangay);

    @Query("SELECT COUNT(u) FROM User u WHERE u.barangay = :barangay")
    long countAllByBarangay(@Param("barangay") String barangay);

    // ── Residents page — paginated search ───────────────────────────────────

    @Query("SELECT u FROM User u WHERE u.barangay = :barangay ORDER BY u.createdAt DESC")
    Page<User> findAllByBarangay(@Param("barangay") String barangay, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.barangay = :barangay AND LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY u.createdAt DESC")
    Page<User> findByBarangayAndName(@Param("barangay") String barangay,
                                     @Param("search") String search,
                                     Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.barangay = :barangay AND LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY u.createdAt DESC")
    Page<User> findByBarangayAndEmail(@Param("barangay") String barangay,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.barangay = :barangay AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY u.createdAt DESC")
    Page<User> findByBarangayAndNameOrEmail(@Param("barangay") String barangay,
                                            @Param("search") String search,
                                            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.barangay = :barangay AND u.role != 'ROLE_ADMIN' ORDER BY u.createdAt DESC")
    List<User> findResidentsByBarangay(@Param("barangay") String barangay);
}