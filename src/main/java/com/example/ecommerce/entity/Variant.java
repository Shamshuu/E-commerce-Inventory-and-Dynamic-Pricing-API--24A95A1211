package com.example.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "variants")
public class Variant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "title")
    private String title;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity = 0;

    @Column(name = "price_adjustment")
    private Double priceAdjustment = 0.0;

    @Column(name = "product_id")
    private Long productId;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Double getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(Double priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
