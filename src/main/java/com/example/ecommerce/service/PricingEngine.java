package com.example.ecommerce.service;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Variant;
import com.example.ecommerce.entity.PricingRule;
import com.example.ecommerce.entity.PricingRuleUsage;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.VariantRepository;
import com.example.ecommerce.repository.PricingRuleRepository;
import com.example.ecommerce.repository.PricingRuleUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.Optional;
import java.time.Instant;

@Service
public class PricingEngine {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VariantRepository variantRepository;
    @Autowired
    private PricingRuleRepository pricingRuleRepository;
    @Autowired
    private PricingRuleUsageRepository pricingRuleUsageRepository;

    public static class PriceResult {
        public double basePrice;
        public double variantAdjustment;
        public List<Map<String, Object>> appliedRules = new ArrayList<>();
        public double finalUnitPrice;
        public double totalPrice;
    }

    /**
     * Calculates price for a product/variant with all rules applied in order.
     * @param productId Product ID
     * @param variantId Variant ID (optional)
     * @param quantity Quantity
     * @param userTier User tier (BRONZE/SILVER/GOLD)
     * @param userId User ID (required when usagePerUser is set on a rule)
     * @param promoCode Promo code (optional)
     * @return PriceResult with breakdown
     */
    public PriceResult calculatePrice(Long productId, Long variantId, int quantity, String userTier, String promoCode, Long userId) {
        PriceResult result = new PriceResult();
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) throw new IllegalArgumentException("Product not found");
        Product product = productOpt.get();
        result.basePrice = product.getBasePrice();
        result.variantAdjustment = 0.0;
        if (variantId != null) {
            Optional<Variant> variantOpt = variantRepository.findById(variantId);
            if (variantOpt.isEmpty()) throw new IllegalArgumentException("Variant not found");
            Variant variant = variantOpt.get();
            result.variantAdjustment = variant.getPriceAdjustment();
        }
        double price = result.basePrice + result.variantAdjustment;

        // 2. Fetch and apply rules in order
        Instant now = Instant.now();
        List<PricingRule> rules = pricingRuleRepository.findActiveValidRules(now); // Optimized query for active rules
        // a. Seasonal/time-based
        for (PricingRule rule : rules) {
            if ("SEASONAL".equalsIgnoreCase(rule.getType()) && Boolean.TRUE.equals(rule.getActive()) &&
                (rule.getStartAt() == null || !now.isBefore(rule.getStartAt())) &&
                (rule.getEndAt() == null || !now.isAfter(rule.getEndAt())) &&
                matchesTarget(rule, productId, variantId, product.getCategoryId()) &&
                usageAllowed(rule, userId)) {
                double discount = rule.getPercentage() != null ? price * (rule.getPercentage() / 100.0) : 0.0;
                price -= discount;
                Map<String, Object> applied = new HashMap<>();
                applied.put("rule_id", rule.getId());
                applied.put("type", "SEASONAL");
                applied.put("discount_amount", discount);
                result.appliedRules.add(applied);
            }
        }
        // b. Bulk discounts
        for (PricingRule rule : rules) {
            if ("BULK".equalsIgnoreCase(rule.getType()) && Boolean.TRUE.equals(rule.getActive()) &&
                rule.getMinQuantity() != null && quantity >= rule.getMinQuantity() &&
                matchesTarget(rule, productId, variantId, product.getCategoryId()) &&
                usageAllowed(rule, userId)) {
                double discount = rule.getPercentage() != null ? price * (rule.getPercentage() / 100.0) : 0.0;
                if (rule.getFlatAmount() != null) discount += rule.getFlatAmount();
                price -= discount;
                Map<String, Object> applied = new HashMap<>();
                applied.put("rule_id", rule.getId());
                applied.put("type", "BULK");
                applied.put("discount_amount", discount);
                result.appliedRules.add(applied);
            }
        }
        // c. User-tier discounts
        for (PricingRule rule : rules) {
            if ("USER_TIER".equalsIgnoreCase(rule.getType()) && Boolean.TRUE.equals(rule.getActive()) &&
                rule.getUserTier() != null && rule.getUserTier().equalsIgnoreCase(userTier) &&
                matchesTarget(rule, productId, variantId, product.getCategoryId()) &&
                usageAllowed(rule, userId)) {
                double discount = rule.getPercentage() != null ? price * (rule.getPercentage() / 100.0) : 0.0;
                if (rule.getFlatAmount() != null) discount += rule.getFlatAmount();
                price -= discount;
                Map<String, Object> applied = new HashMap<>();
                applied.put("rule_id", rule.getId());
                applied.put("type", "USER_TIER");
                applied.put("discount_amount", discount);
                result.appliedRules.add(applied);
            }
        }
        // d. Promo code rules
        for (PricingRule rule : rules) {
            if ("PROMO".equalsIgnoreCase(rule.getType()) && Boolean.TRUE.equals(rule.getActive()) &&
                rule.getPromoCode() != null && promoCode != null && rule.getPromoCode().equalsIgnoreCase(promoCode) &&
                matchesTarget(rule, productId, variantId, product.getCategoryId()) &&
                usageAllowed(rule, userId)) {
                double discount = rule.getPercentage() != null ? price * (rule.getPercentage() / 100.0) : 0.0;
                if (rule.getFlatAmount() != null) discount += rule.getFlatAmount();
                price -= discount;
                Map<String, Object> applied = new HashMap<>();
                applied.put("rule_id", rule.getId());
                applied.put("type", "PROMO_CODE");
                applied.put("discount_amount", discount);
                result.appliedRules.add(applied);
            }
        }
        // 3. Calculate finalUnitPrice and totalPrice
        result.finalUnitPrice = Math.max(price, 0.0); // never negative
        result.totalPrice = result.finalUnitPrice * quantity;
        return result;
    }

    private boolean matchesTarget(PricingRule rule, Long productId, Long variantId, Long categoryId) {
        if ("PRODUCT".equalsIgnoreCase(rule.getTargetType()) && rule.getTargetId() != null && rule.getTargetId().equals(productId)) return true;
        if ("VARIANT".equalsIgnoreCase(rule.getTargetType()) && rule.getTargetId() != null && variantId != null && rule.getTargetId().equals(variantId)) return true;
        if ("CATEGORY".equalsIgnoreCase(rule.getTargetType()) && rule.getTargetId() != null && rule.getTargetId().equals(categoryId)) return true;
        return false;
    }

    private boolean usageAllowed(PricingRule rule, Long userId) {
        // Enforce total usage limit
        if (rule.getUsageLimit() != null) {
            long totalUsed = pricingRuleUsageRepository.sumUsageByRuleId(rule.getId());
            if (totalUsed >= rule.getUsageLimit()) {
                return false;
            }
        }
        // Enforce per-user limit
        if (rule.getUsagePerUser() != null) {
            if (userId == null) {
                throw new IllegalArgumentException("User ID is required for rules with usagePerUser");
            }
            long userUsed = pricingRuleUsageRepository.sumUsageByRuleIdAndUserId(rule.getId(), userId);
            if (userUsed >= rule.getUsagePerUser()) {
                return false;
            }
        }
        return true;
    }
}