package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.Favor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavorRepository extends JpaRepository<Favor, Long> {

    // Fetch open favors filtered by barangay
    Page<Favor> findByStatusAndBarangay(String status, String barangay, Pageable pageable);

    // Fetch open favors filtered by barangay and category
    Page<Favor> findByStatusAndBarangayAndCategory(String status, String barangay, String category, Pageable pageable);

    // Count by requester
    long countByRequesterId(Long requesterId);

    // Count completed by claimer
    long countByClaimerIdAndStatus(Long claimerId, String status);
}