package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT r FROM Reservation r WHERE r.expiresAt < :now AND r.released = false")
    List<Reservation> findExpiredUnreleased(Instant now);
    
    @Query("SELECT r FROM Reservation r WHERE r.cartItemId = :cartItemId AND r.released = false")
    List<Reservation> findByCartItemIdAndNotReleased(Long cartItemId);
}
