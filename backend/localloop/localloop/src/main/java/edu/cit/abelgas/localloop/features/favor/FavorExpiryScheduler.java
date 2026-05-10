package edu.cit.abelgas.localloop.scheduler;

import edu.cit.abelgas.localloop.entity.Favor;
import edu.cit.abelgas.localloop.repository.FavorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs once daily at midnight to expire OPEN favors
 * whose dateNeeded has already passed.
 *
 * Lifecycle rule: OPEN → EXPIRED (when dateNeeded < today)
 */
@Component
public class FavorExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(FavorExpiryScheduler.class);

    private final FavorRepository favorRepository;

    public FavorExpiryScheduler(FavorRepository favorRepository) {
        this.favorRepository = favorRepository;
    }

    /**
     * Runs every day at midnight (00:00).
     * Finds all OPEN favors with a dateNeeded before today
     * and marks them as EXPIRED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueFavors() {
        LocalDate today = LocalDate.now();
        List<Favor> expirable = favorRepository.findExpirableFavors(today);

        if (expirable.isEmpty()) {
            log.info("[FavorExpiry] No favors to expire today ({})", today);
            return;
        }

        expirable.forEach(f -> f.setStatus("EXPIRED"));
        favorRepository.saveAll(expirable);

        log.info("[FavorExpiry] Expired {} favor(s) on {}", expirable.size(), today);
    }
}