package com.stary.backend.api.products.repositories;

import com.stary.backend.api.products.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Simple text search + filters (basic)
    @Query("SELECT p FROM Product p " +
            "WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%',:search,'%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:condition IS NULL OR p.condition = :condition) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> searchFiltered(@Param("search") String search,
                                 @Param("category") String category,
                                 @Param("condition") String condition,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice);
    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY p.name ASC
    """)
    List<Product> suggest(@Param("q") String q, Pageable pageable);
}
