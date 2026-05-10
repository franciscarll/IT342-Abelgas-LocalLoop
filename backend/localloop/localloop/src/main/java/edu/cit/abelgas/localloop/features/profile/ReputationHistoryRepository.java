package edu.cit.abelgas.localloop.features.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReputationHistoryRepository extends JpaRepository<ReputationHistory, Long> {

    /**
     * Returns all reputation history entries for a user,
     * ordered newest first.
     */
    List<ReputationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}