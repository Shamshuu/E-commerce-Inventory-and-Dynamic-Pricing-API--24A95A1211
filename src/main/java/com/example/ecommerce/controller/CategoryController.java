package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Category;
import com.example.ecommerce.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    @Autowired
    private CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping
    public List<CategoryNode> listCategories() {
        List<Category> all = categoryRepository.findAll();
        Map<Long, CategoryNode> map = new HashMap<>();
        List<CategoryNode> roots = new ArrayList<>();
        for (Category c : all) {
            map.put(c.getId(), new CategoryNode(c));
        }
        for (CategoryNode node : map.values()) {
            if (node.parentId != null && map.containsKey(node.parentId)) {
                map.get(node.parentId).children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable Long id) {
        return categoryRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return categoryRepository.findById(id)
            .map(existing -> {
                category.setId(id);
                Category updated = categoryRepository.save(category);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

class CategoryNode {
    public Long id;
    public String name;
    public String slug;
    public Long parentId;
    public List<CategoryNode> children = new ArrayList<>();

    CategoryNode(Category c) {
        this.id = c.getId();
        this.name = c.getName();
        this.slug = c.getSlug();
        this.parentId = c.getParentId();
    }
}
