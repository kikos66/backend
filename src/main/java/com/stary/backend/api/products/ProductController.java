package com.stary.backend.api.products;


import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService svc;

    public ProductController(ProductService svc) { this.svc = svc; }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return svc.findById(id)
                .map(p -> ResponseEntity.ok(p))
                .orElse(ResponseEntity.notFound().build());
    }

    // Create product with multipart form-data
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam String condition,
            @RequestPart(required = false) MultipartFile[] images
    ) {
        try{
            Product p = new Product();
            p.setName(name);
            p.setDescription(description);
            p.setPrice(price);
            p.setCategory(category);
            p.setCondition(condition);

            Product saved = svc.createProduct(p, images);
            return ResponseEntity.status(201).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to store images");
        }
    }

    // Upload additional images to an existing product
    @PostMapping(value="/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addImages(@PathVariable Long id, @RequestPart MultipartFile[] images) {
        try {
            Product p = svc.findById(id).orElseThrow();
            svc.assertOwner(p);
            svc.saveImagesForProduct(p, images);
            return ResponseEntity.ok(p);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to store images");
        }
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        return ResponseEntity.ok(
                svc.searchFilteredPaged(search, category, condition, minPrice, maxPrice, page, size)
        );
    }

    @GetMapping("/suggest")
    public List<Product> suggest(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        return svc.suggest(q);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            svc.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body("Forbidden");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Product>> myProducts() {
        return ResponseEntity.ok(svc.findMine());
    }
}
