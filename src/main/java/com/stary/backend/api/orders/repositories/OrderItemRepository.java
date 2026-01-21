package com.stary.backend.api.orders.repositories;

import com.stary.backend.api.orders.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Modifying
    @Query("""
        DELETE FROM OrderItem oi
        WHERE oi.order.id IN (
            SELECT o.id FROM PurchaseOrder o WHERE o.buyer.id = :buyerId
        )
    """)
    void deleteItemsByBuyerId(@Param("buyerId") Long buyerId);
}
