package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Variant;
import com.example.ecommerce.repository.VariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/variants")
public class VariantController {
    @Autowired
    private VariantRepository variantRepository;

    @GetMapping
    public List<Variant> listVariants() {
        return variantRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Variant> getVariant(@PathVariable Long id) {
        return variantRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Variant> updateVariant(@PathVariable Long id, @RequestBody Variant variant) {
        return variantRepository.findById(id)
            .map(existing -> {
                if (variant.getStockQuantity() != null) existing.setStockQuantity(variant.getStockQuantity());
                if (variant.getPriceAdjustment() != null) existing.setPriceAdjustment(variant.getPriceAdjustment());
                Variant updated = variantRepository.save(existing);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
