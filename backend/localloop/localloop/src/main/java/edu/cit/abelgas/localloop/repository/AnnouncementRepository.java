package edu.cit.abelgas.localloop.repository;

import edu.cit.abelgas.localloop.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByBarangayOrderByCreatedAtDesc(String barangay, Pageable pageable);
}