package com.example.ecommerce.service;

import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Variant;
import com.example.ecommerce.entity.Reservation;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.VariantRepository;
import com.example.ecommerce.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;

@Service
public class CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private VariantRepository variantRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PricingEngine pricingEngine;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Adds a variant to the cart, reserves inventory, creates a reservation, and snapshots the price and discounts.
     * Ensures concurrency safety and links the reservation to the cart item.
     * @param cartId Cart ID
     * @param variantId Variant ID
     * @param quantity Quantity to add
     * @param userTier User tier for pricing
     * @param promoCode Optional promo code
     * @return The created CartItem with price snapshot and reservation
     * @throws IllegalArgumentException if cart or variant not found
     * @throws IllegalStateException if insufficient stock
     */
    @Transactional
    public CartItem addItemToCart(Long cartId, Long variantId, int quantity, String userTier, String promoCode) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));
        Variant variant = variantRepository.findWithLockingById(variantId)
            .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId));
        int available = variant.getStockQuantity() - variant.getReservedQuantity();
        if (available < quantity) {
            throw new IllegalStateException("Insufficient available stock for variant: " + variantId);
        }
        // Reserve inventory
        variant.setReservedQuantity(variant.getReservedQuantity() + quantity);
        variantRepository.save(variant);
        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setVariantId(variantId);
        reservation.setQuantity(quantity);
        reservation.setExpiresAt(Instant.now().plusSeconds(900)); // 15 min
        reservation.setReleased(false);
        reservation = reservationRepository.save(reservation);
        logger.info("Reservation created: reservationId={}, variantId={}, quantity={}, expiresAt={}", 
            reservation.getId(), variantId, quantity, reservation.getExpiresAt());
        // Calculate price snapshot
        PricingEngine.PriceResult priceResult = pricingEngine.calculatePrice(variant.getProductId(), variantId, quantity, userTier, promoCode, cart.getUserId());
        CartItem item = new CartItem();
        item.setCartId(cartId);
        item.setVariantId(variantId);
        item.setQuantity(quantity);
        item.setUnitPrice(priceResult.finalUnitPrice);
        try {
            item.setDiscounts(objectMapper.writeValueAsString(priceResult.appliedRules));
        } catch (Exception e) {
            logger.warn("Failed to serialize discounts to JSON, using toString: {}", e.getMessage());
            item.setDiscounts(priceResult.appliedRules.toString());
        }
        item.setSubtotal(priceResult.totalPrice);
        item.setSnapshotAt(Instant.now());
        item = cartItemRepository.save(item);
        // Link reservation to cart item
        reservation.setCartItemId(item.getId());
        reservationRepository.save(reservation);
        return item;
    }

    /**
     * Updates the quantity of a cart item, adjusts reservation quantity, and recalculates subtotal.
     * Extends reservation expiry time.
     * @param cartItemId Cart item ID
     * @param newQuantity New quantity
     * @return Updated CartItem
     * @throws IllegalArgumentException if cart item not found
     * @throws IllegalStateException if insufficient stock for increase
     */
    @Transactional
    public CartItem updateCartItemQuantity(Long cartItemId, int newQuantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        int quantityDiff = newQuantity - item.getQuantity();
        
        if (quantityDiff != 0) {
            // Find associated reservations
            List<Reservation> reservations = reservationRepository.findByCartItemIdAndNotReleased(cartItemId);
            if (reservations.isEmpty()) {
                throw new IllegalStateException("No active reservations found for cart item: " + cartItemId);
            }
            
            // Lock variant for update
            Variant variant = variantRepository.findWithLockingById(item.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + item.getVariantId()));
            
            if (quantityDiff > 0) {
                // Increasing quantity - check available stock
                int available = variant.getStockQuantity() - variant.getReservedQuantity();
                if (available < quantityDiff) {
                    throw new IllegalStateException("Insufficient available stock. Available: " + available + ", Requested: " + quantityDiff);
                }
                // Increase reserved quantity
                variant.setReservedQuantity(variant.getReservedQuantity() + quantityDiff);
            } else {
                // Decreasing quantity - release reserved quantity
                variant.setReservedQuantity(variant.getReservedQuantity() + quantityDiff); // quantityDiff is negative
            }
            
            variantRepository.save(variant);
            
            // Update reservation quantities (distribute across reservations if multiple)
            int remainingDiff = quantityDiff;
            for (Reservation reservation : reservations) {
                if (remainingDiff == 0) break;
                
                if (remainingDiff > 0) {
                    // Increase this reservation
                    reservation.setQuantity(reservation.getQuantity() + remainingDiff);
                    remainingDiff = 0;
                } else {
                    // Decrease this reservation
                    int decreaseAmount = Math.min(Math.abs(remainingDiff), reservation.getQuantity());
                    reservation.setQuantity(reservation.getQuantity() - decreaseAmount);
                    remainingDiff += decreaseAmount;
                    
                    if (reservation.getQuantity() == 0) {
                        reservation.setReleased(true);
                    }
                }
                
                // Extend reservation expiry
                reservation.setExpiresAt(Instant.now().plusSeconds(900)); // 15 min
                reservationRepository.save(reservation);
            }
        }
        
        // Recalculate subtotal using existing unit price (price snapshot)
        item.setQuantity(newQuantity);
        item.setSubtotal(item.getUnitPrice() * newQuantity);
        item = cartItemRepository.save(item);
        
        return item;
    }

    /**
     * Removes a cart item and releases associated reservations.
     * @param cartItemId Cart item ID to remove
     * @throws IllegalArgumentException if cart item not found
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        
        // Find associated reservations
        List<Reservation> reservations = reservationRepository.findByCartItemIdAndNotReleased(cartItemId);
        
        if (!reservations.isEmpty()) {
            // Lock variant for update
            Variant variant = variantRepository.findWithLockingById(item.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + item.getVariantId()));
            
            // Release all reservations
            int totalReleasedQuantity = 0;
            for (Reservation reservation : reservations) {
                totalReleasedQuantity += reservation.getQuantity();
                reservation.setReleased(true);
                reservationRepository.save(reservation);
                logger.info("Reservation released: reservationId={}, variantId={}, quantity={}", 
                    reservation.getId(), reservation.getVariantId(), reservation.getQuantity());
            }
            
            // Decrease reserved quantity
            variant.setReservedQuantity(variant.getReservedQuantity() - totalReleasedQuantity);
            variantRepository.save(variant);
        }
        
        // Delete cart item
        cartItemRepository.delete(item);
    }
}
