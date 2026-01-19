package com.stary.backend.api.products.repositories;

import com.stary.backend.api.products.Product;
import com.stary.backend.api.users.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
                                 @Param("maxPrice") Double maxPrice, Long ownerId);

    @Query("""
        SELECT p FROM Product p
        WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:category IS NULL OR p.category = :category)
        AND (:condition IS NULL OR p.condition = :condition)
        AND (:excludeOwnerId IS NULL OR p.owner.id <> :excludeOwnerId)
    """)
    List<Product> searchFilteredExcludeOwner(
            String search,
            String category,
            String condition,
            Long excludeOwnerId
    );

    List<Product> findByOwnerId(Long ownerId);

    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY p.name ASC
    """)
    List<Product> suggest(@Param("q") String q, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM Product p WHERE p.owner = :owner")
    void deleteByOwner(@Param("owner") User owner);
}
