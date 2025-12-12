package com.example.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.ecommerce.entity.PricingRule;

import java.time.Instant;
import java.util.List;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    // Custom queries for rule selection are implemented below.

    // Fetch only active and currently valid rules
    @Query("SELECT r FROM PricingRule r WHERE r.active = true AND (r.startAt IS NULL OR r.startAt <= :now) AND (r.endAt IS NULL OR r.endAt >= :now)")
    List<PricingRule> findActiveValidRules(@Param("now") Instant now);
}
