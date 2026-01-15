package com.stary.backend.api.products;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService svc;
    public ProductController(ProductService svc) { this.svc = svc; }

    @GetMapping
    public ResponseEntity<List<Product>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {
        List<Product> results = svc.list(search, category, condition, minPrice, maxPrice);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getOne(@PathVariable Long id) {
        Product p = svc.get(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    // Create product - POST should be protected (only authenticated users)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product payload) {
        Product saved = svc.create(payload);
        return ResponseEntity.status(201).body(saved);
    }
}