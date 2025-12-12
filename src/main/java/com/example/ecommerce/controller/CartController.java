package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<Cart> createOrGetActiveCart(@RequestParam Long userId) {
        Cart active = cartRepository.findByUserIdAndStatus(userId, "ACTIVE").orElse(null);
        if (active != null) {
            return ResponseEntity.status(200).body(active);
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus("ACTIVE");
        Cart saved = cartRepository.save(cart);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cart> getCart(@PathVariable Long id) {
        return cartRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkoutCart(@RequestParam Long cartId, @RequestBody List<Long> reservationIds) {
        checkoutService.checkoutCart(cartId, reservationIds);
        return ResponseEntity.ok("Checkout successful");
    }
}
