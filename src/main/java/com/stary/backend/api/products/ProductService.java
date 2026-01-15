package com.stary.backend.api.products;

import com.stary.backend.api.products.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repo;
    public ProductService(ProductRepository repo) { this.repo = repo; }

    public List<Product> list(String search, String category, String condition, Double minPrice, Double maxPrice) {
        return repo.searchFiltered(
                (search == null || search.isBlank()) ? null : search,
                (category == null || category.isBlank()) ? null : category,
                (condition == null || condition.isBlank()) ? null : condition,
                minPrice, maxPrice);
    }

    public Product create(Product p) {
        // optionally validate fields
        return repo.save(p);
    }

    public Product get(Long id) {
        return repo.findById(id).orElse(null);
    }
}