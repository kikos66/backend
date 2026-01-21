package com.stary.backend.api.orders;

import com.stary.backend.api.orders.repositories.OrderItemRepository;
import com.stary.backend.api.orders.repositories.PurchaseOrderRepository;
import com.stary.backend.api.products.Product;
import com.stary.backend.api.products.repositories.ProductRepository;
import com.stary.backend.api.products.ProductService;
import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final PurchaseOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(ProductRepository productRepository,
                        ProductService productService,
                        UserRepository userRepository,
                        PurchaseOrderRepository orderRepository,
                        OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public OrderSummaryDTO checkout(List<CartItemDTO> items) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        User buyer = userRepository.findByEmail(auth.getName()).orElseThrow();

        // aggregate product quantities
        Map<Long, Integer> aggregated = new HashMap<>();
        for (CartItemDTO it : items) {
            aggregated.merge(it.getProductId(), it.getQuantity(), Integer::sum);
        }

        // Validate stock first
        for (Map.Entry<Long, Integer> e : aggregated.entrySet()) {
            Long pid = e.getKey();
            int qty = e.getValue();

            Product p = productRepository.findById(pid)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));

            int current = p.getQuantity() == null ? 0 : p.getQuantity();
            if (current < qty) {
                throw new IllegalArgumentException("Not enough stock for product: " + p.getName());
            }
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setBuyer(buyer);
        order.setCreatedAt(Instant.now());

        double total = 0.0;
        List<Map<String, Object>> resultItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> e : aggregated.entrySet()) {
            Long pid = e.getKey();
            int qty = e.getValue();

            Product p = productRepository.findById(pid).orElseThrow();
            // reduce stock (existing productService method)
            productService.reduceStock(pid, qty);

            double price = p.getPrice() == null ? 0.0 : p.getPrice();
            double subtotal = price * qty;
            total += subtotal;

            // create persistent OrderItem
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductPrice(price);
            oi.setQuantity(qty);
            oi.setSubtotal(subtotal);
            if (p.getOwner() != null) {
                oi.setProductOwnerId(p.getOwner().getId());
                oi.setProductOwnerName(p.getOwner().getUsername());
            }
            order.getItems().add(oi);
        }

        order.setTotal(total);
        PurchaseOrder saved = orderRepository.save(order);
        // order items saved via cascade

        // Build response DTO (same shape you used before)
        OrderSummaryDTO summary = new OrderSummaryDTO();
        summary.setTotal(total);

        List<Map<String, Object>> itemsForDto = new ArrayList<>();
        for (OrderItem oi : saved.getItems()) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", oi.getProductId());
            m.put("name", oi.getProductName());
            m.put("quantity", oi.getQuantity());
            m.put("price", oi.getProductPrice());
            m.put("subtotal", oi.getSubtotal());
            itemsForDto.add(m);
        }
        summary.setItems(itemsForDto);
        summary.setMessage("Purchase simulated — stock updated and order saved. Thank you!");

        return summary;
    }

    public List<PurchaseOrder> findOrdersForBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public List<PurchaseOrder> findSalesForSeller(Long sellerId) {
        return orderRepository.findSalesForSeller(sellerId);
    }
}