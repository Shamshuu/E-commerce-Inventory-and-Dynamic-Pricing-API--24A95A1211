package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Variant> findWithLockingById(Long id);
}
