package com.stary.backend.api.orders;

import com.stary.backend.api.products.Product;
import com.stary.backend.api.products.repositories.ProductRepository;
import com.stary.backend.api.products.ProductService;
import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderService {
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final UserRepository userRepository;

    public OrderService(ProductRepository productRepository,
                        ProductService productService,
                        UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderSummaryDTO checkout(List<CartItemDTO> items) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        User buyer = userRepository.findByEmail(auth.getName()).orElseThrow();

        Map<Long, Integer> aggregated = new HashMap<>();
        for (CartItemDTO it : items) {
            aggregated.merge(it.getProductId(), it.getQuantity(), Integer::sum);
        }

        List<Map<String, Object>> resultItems = new ArrayList<>();
        double total = 0.0;

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

        // All good -> decrement
        for (Map.Entry<Long, Integer> e : aggregated.entrySet()) {
            Long pid = e.getKey();
            int qty = e.getValue();
            Product p = productRepository.findById(pid).orElseThrow();
            productService.reduceStock(pid, qty);

            double price = p.getPrice() == null ? 0.0 : p.getPrice();
            double subtotal = price * qty;
            total += subtotal;

            Map<String, Object> item = new HashMap<>();
            item.put("productId", pid);
            item.put("name", p.getName());
            item.put("quantity", qty);
            item.put("price", price);
            item.put("subtotal", subtotal);
            resultItems.add(item);
        }

        OrderSummaryDTO summary = new OrderSummaryDTO();
        summary.setTotal(total);
        summary.setItems(resultItems);
        summary.setMessage("Purchase simulated — stock updated. Thank you!");

        return summary;
    }
}
