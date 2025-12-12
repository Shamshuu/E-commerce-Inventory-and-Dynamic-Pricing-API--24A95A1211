package com.example.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pricing_rule_usages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rule_id", "user_id"})
})
public class PricingRuleUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "used_count", nullable = false)
    private Long usedCount = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Long usedCount) {
        this.usedCount = usedCount;
    }
}

