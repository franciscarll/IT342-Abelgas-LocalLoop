package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.response.AnnouncementResponse;
import edu.cit.abelgas.localloop.entity.Announcement;
import edu.cit.abelgas.localloop.entity.User;
import edu.cit.abelgas.localloop.repository.AnnouncementRepository;
import edu.cit.abelgas.localloop.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    public Page<AnnouncementResponse> getAnnouncements(String barangay, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return announcementRepository
                .findByBarangayOrderByCreatedAtDesc(barangay, pageable)
                .map(this::toResponse);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        String username = userRepository.findById(a.getPostedBy())
                .map(User::getName)
                .orElse("Unknown User");

        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .barangay(a.getBarangay())
                .postedBy(username)
                .createdAt(a.getCreatedAt())
                .build();
    }
}