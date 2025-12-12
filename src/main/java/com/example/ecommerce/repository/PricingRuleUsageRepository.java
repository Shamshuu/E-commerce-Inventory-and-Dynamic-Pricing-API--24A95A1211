package com.example.ecommerce.repository;

import com.example.ecommerce.entity.PricingRuleUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PricingRuleUsageRepository extends JpaRepository<PricingRuleUsage, Long> {
    Optional<PricingRuleUsage> findByRuleIdAndUserId(Long ruleId, Long userId);

    @Query("SELECT COALESCE(SUM(u.usedCount),0) FROM PricingRuleUsage u WHERE u.ruleId = :ruleId")
    long sumUsageByRuleId(@Param("ruleId") Long ruleId);

    @Query("SELECT COALESCE(SUM(u.usedCount),0) FROM PricingRuleUsage u WHERE u.ruleId = :ruleId AND u.userId = :userId")
    long sumUsageByRuleIdAndUserId(@Param("ruleId") Long ruleId, @Param("userId") Long userId);
}

