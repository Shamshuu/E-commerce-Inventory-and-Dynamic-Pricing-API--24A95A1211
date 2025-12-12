package com.example.ecommerce.controller;

import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/cart/items")
public class CartItemController {
    @Autowired
    private CartService cartService;

    @PostMapping
    public ResponseEntity<CartItem> addItem(@RequestBody Map<String, Object> body) {
        Long cartId = ((Number) body.get("cartId")).longValue();
        Long variantId = ((Number) body.get("variantId")).longValue();
        int quantity = ((Number) body.get("quantity")).intValue();
        String userTier = (String) body.get("userTier");
        String promoCode = body.get("promoCode") != null ? (String) body.get("promoCode") : null;
        CartItem item = cartService.addItemToCart(cartId, variantId, quantity, userTier, promoCode);
        return ResponseEntity.status(201).body(item);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CartItem> updateItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int quantity = ((Number) body.get("quantity")).intValue();
        CartItem updated = cartService.updateCartItemQuantity(id, quantity);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        cartService.removeCartItem(id);
        return ResponseEntity.noContent().build();
    }
}
