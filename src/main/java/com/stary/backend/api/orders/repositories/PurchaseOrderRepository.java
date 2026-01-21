package com.stary.backend.api.orders.repositories;

import com.stary.backend.api.orders.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @Query("SELECT DISTINCT o FROM PurchaseOrder o JOIN o.items i" +
            " WHERE i.productOwnerId = :sellerId ORDER BY o.createdAt DESC")
    List<PurchaseOrder> findSalesForSeller(@Param("sellerId") Long sellerId);

    void deleteByBuyerId(Long buyerId);
}