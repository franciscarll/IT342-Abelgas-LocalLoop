package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.User;
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

    /**
     * Count all non-admin users in the given barangay.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.barangay = :barangay AND u.role != 'ROLE_ADMIN'")
    long countResidentsByBarangay(@Param("barangay") String barangay);

    /**
     * Sum of all reputation scores for users in the given barangay.
     * Returns 0 if no users exist.
     */
    @Query("SELECT COALESCE(SUM(u.reputationScore), 0) FROM User u WHERE u.barangay = :barangay")
    long sumReputationByBarangay(@Param("barangay") String barangay);

    /**
     * Returns all non-admin users in the barangay (for Residents page later).
     */
    @Query("SELECT u FROM User u WHERE u.barangay = :barangay AND u.role != 'ROLE_ADMIN' ORDER BY u.createdAt DESC")
    List<User> findResidentsByBarangay(@Param("barangay") String barangay);
}