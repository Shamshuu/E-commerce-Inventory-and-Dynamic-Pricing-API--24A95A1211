package com.example.ecommerce.service;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);
    
    @Autowired
    private VariantRepository variantRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private PricingRuleUsageRepository pricingRuleUsageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Atomically checks out a cart: validates reservations, decrements stock, releases reservations, updates cart, creates order.
     * @param cartId Cart ID to checkout
     * @param reservationIds List of reservation IDs to checkout
     */
    @Transactional
    public void checkoutCart(Long cartId, List<Long> reservationIds) {
        try {
            // Validate cart
            Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

            double totalOrderAmount = 0.0;

            for (Long reservationId : reservationIds) {
                Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
                if (Boolean.TRUE.equals(reservation.getReleased())) {
                    String errorMsg = "Reservation already released: " + reservationId;
                    logger.error("Checkout failed for cart {}: {}", cartId, errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                // Ensure reservation belongs to cart via cart item -> cart
                CartItem cartItem = cartItemRepository.findById(reservation.getCartItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Cart item not found for reservation: " + reservationId));
                if (!cartItem.getCartId().equals(cartId)) {
                    String errorMsg = "Reservation " + reservationId + " does not belong to cart " + cartId;
                    logger.error("Checkout failed for cart {}: {}", cartId, errorMsg);
                    throw new IllegalStateException(errorMsg);
                }

                // Lock the variant row for update using SELECT ... FOR UPDATE
                Variant variant = variantRepository.findWithLockingById(reservation.getVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + reservation.getVariantId()));
                if (variant.getStockQuantity() < reservation.getQuantity()) {
                    String errorMsg = "Insufficient stock for variant: " + variant.getId() + ", required: " + reservation.getQuantity() + ", available: " + variant.getStockQuantity();
                    logger.error("Checkout failed for cart {}: {}", cartId, errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                // Decrement stock, release reservation
                variant.setStockQuantity(variant.getStockQuantity() - reservation.getQuantity());
                variant.setReservedQuantity(variant.getReservedQuantity() - reservation.getQuantity());
                reservation.setReleased(true);
                variantRepository.save(variant);
                reservationRepository.save(reservation);

                // Accumulate total using snapshot pricing
                totalOrderAmount += cartItem.getUnitPrice() * cartItem.getQuantity();

                // Track pricing rule usage based on discounts JSON
                updateRuleUsageFromCartItem(cart.getUserId(), cartItem);
            }
            // Update cart status
            cart.setStatus("CHECKED_OUT");
            cartRepository.save(cart);
            // Create order
            Order order = new Order();
            order.setCartId(cartId);
            order.setTotal(totalOrderAmount);
            orderRepository.save(order);
            logger.info("Checkout successful for cart {}: orderId={}, total={}", cartId, order.getId(), totalOrderAmount);
        } catch (Exception e) {
            logger.error("Checkout failed for cart {}: {}", cartId, e.getMessage(), e);
            throw e;
        }
    }

    private void updateRuleUsageFromCartItem(Long userId, CartItem cartItem) {
        if (cartItem.getDiscounts() == null || cartItem.getDiscounts().isEmpty()) return;
        try {
            List<Map<String, Object>> discounts = objectMapper.readValue(cartItem.getDiscounts(), List.class);
            for (Map<String, Object> d : discounts) {
                Object ruleIdObj = d.get("rule_id");
                if (ruleIdObj == null) continue;
                Long ruleId = ((Number) ruleIdObj).longValue();
                PricingRuleUsage usage = pricingRuleUsageRepository.findByRuleIdAndUserId(ruleId, userId)
                    .orElseGet(() -> {
                        PricingRuleUsage u = new PricingRuleUsage();
                        u.setRuleId(ruleId);
                        u.setUserId(userId);
                        u.setUsedCount(0L);
                        return u;
                    });
                usage.setUsedCount(usage.getUsedCount() + 1);
                pricingRuleUsageRepository.save(usage);
            }
        } catch (Exception e) {
            logger.error("Failed to update pricing rule usage for cartItem {}: {}", cartItem.getId(), e.getMessage());
            throw new IllegalStateException("Failed to update pricing rule usage", e);
        }
    }
}
