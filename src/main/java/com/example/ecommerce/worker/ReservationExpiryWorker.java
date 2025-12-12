package com.example.ecommerce.worker;

import com.example.ecommerce.entity.Reservation;
import com.example.ecommerce.entity.Variant;
import com.example.ecommerce.repository.ReservationRepository;
import com.example.ecommerce.repository.VariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryWorker {
    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiryWorker.class);
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private VariantRepository variantRepository;

    // Runs every minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireReservations() {
        String lockKey = "reservation_expiry_lock:" + java.time.LocalDateTime.now().withSecond(0).withNano(0);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", java.time.Duration.ofMinutes(1));
        if (Boolean.FALSE.equals(acquired)) {
            logger.info("Reservation expiry worker already running for this minute.");
            return;
        }
        try {
            // 1. Find all reservations where expiresAt < now AND released = false
            List<Reservation> expired = reservationRepository.findExpiredUnreleased(Instant.now());
            // 2. For each, release reserved quantity and mark as released
            for (Reservation r : expired) {
                try {
                    Variant v = variantRepository.findWithLockingById(r.getVariantId())
                        .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + r.getVariantId()));
                    v.setReservedQuantity(v.getReservedQuantity() - r.getQuantity());
                    r.setReleased(true);
                    variantRepository.save(v);
                    reservationRepository.save(r);
                    logger.info("Released reservation {} for variant {} (qty {})", r.getId(), r.getVariantId(), r.getQuantity());
                } catch (Exception e) {
                    logger.error("Error releasing reservation {}: {}", r.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Reservation expiry worker failed: {}", e.getMessage());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
