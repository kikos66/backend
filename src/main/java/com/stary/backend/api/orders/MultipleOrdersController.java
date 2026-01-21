package com.stary.backend.api.orders;

import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class MultipleOrdersController {
    private final OrderService orderService;
    private final UserRepository userRepository;

    public MultipleOrdersController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).body("Not authenticated");
        User u = userRepository.findByEmail(auth.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("User not found");
        List<PurchaseOrder> list = orderService.findOrdersForBuyer(u.getId());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/sales")
    public ResponseEntity<?> mySales() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).body("Not authenticated");
        User u = userRepository.findByEmail(auth.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("User not found");
        List<PurchaseOrder> list = orderService.findSalesForSeller(u.getId());
        return ResponseEntity.ok(list);
    }
}