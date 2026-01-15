package com.stary.backend.api.products;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double price;

    @Column(name = "category_name")
    private String category;

    @Column(name = "product_condition")
    private String condition; // new | used | refurbished

    // You can add image URL, quantity, createdBy, etc.
}
