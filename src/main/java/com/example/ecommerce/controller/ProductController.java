package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Variant;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.VariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.ecommerce.service.PricingEngine;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VariantRepository variantRepository;
    @Autowired
    private PricingEngine pricingEngine;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<Variant> createVariant(@PathVariable Long productId, @RequestBody Variant variant) {
        if (!productRepository.existsById(productId)) {
            return ResponseEntity.notFound().build();
        }
        variant.setProductId(productId);
        Variant saved = variantRepository.save(variant);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/{productId}/price")
    public ResponseEntity<PricingEngine.PriceResult> getPrice(
            @PathVariable Long productId,
            @RequestParam int quantity,
            @RequestParam String userTier,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String promoCode,
            @RequestParam(required = false) Long userId) {
        PricingEngine.PriceResult result = pricingEngine.calculatePrice(productId, variantId, quantity, userTier, promoCode, userId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productRepository.findById(id)
            .map(existing -> {
                product.setId(id);
                Product updated = productRepository.save(product);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveProduct(@PathVariable Long id) {
        return productRepository.findById(id)
            .map(product -> {
                product.setStatus("ARCHIVED");
                productRepository.save(product);
                return ResponseEntity.noContent().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
