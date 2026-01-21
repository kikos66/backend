package com.stary.backend.api.products;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.stary.backend.api.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1; // number in stock, default 1

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ProductImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER) // EAGER so owner is serialized safely
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"password"}) // avoid sending password if any (safety)
    private User owner;
}
