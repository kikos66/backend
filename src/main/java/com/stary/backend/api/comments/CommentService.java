package com.stary.backend.api.comments;

import com.stary.backend.api.products.Product;
import com.stary.backend.api.products.ProductService;
import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CommentService {
    private final CommentRepository repo;
    private final ProductService productService;
    private final UserRepository userRepository;

    public CommentService(CommentRepository repo, ProductService productService, UserRepository userRepository) {
        this.repo = repo;
        this.productService = productService;
        this.userRepository = userRepository;
    }

    public List<Comment> listForProduct(Long productId) {
        return repo.findByProductIdOrderByCreatedAtAsc(productId);
    }

    public Page<Comment> listForProductPaged(Long productId, int page, int size) {
        return repo.findByProductIdOrderByCreatedAtAsc(
                productId,
                PageRequest.of(page, size)
        );
    }

    @Transactional
    public Comment create(Long productId, String content) {
        Product product = productService.findById(productId).orElseThrow(() -> new NoSuchElementException("Product not found"));
        User author = getAuthenticatedUser();
        Comment c = new Comment();
        c.setProduct(product);
        c.setAuthor(author);
        c.setContent(content);
        return repo.save(c);
    }

    @Transactional
    public Comment edit(Long commentId, String content) {
        Comment c = repo.findById(commentId).orElseThrow();
        User user = getAuthenticatedUser();
        if (!c.getAuthor().getId().equals(user.getId()) && !isModeratorOrAdmin(user)) {
            throw new SecurityException("Not allowed");
        }
        c.setContent(content);
        c.setUpdatedAt(java.time.Instant.now());
        return repo.save(c);
    }

    @Transactional
    public void delete(Long commentId) {
        Comment c = repo.findById(commentId).orElseThrow();
        User user = getAuthenticatedUser();
        if (!c.getAuthor().getId().equals(user.getId()) && !isModeratorOrAdmin(user)) {
            throw new SecurityException("Not allowed");
        }
        repo.delete(c);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    private boolean isModeratorOrAdmin(User u) {
        return u.getRole() == com.stary.backend.api.users.Role.ROLE_MODERATOR ||
                u.getRole() == com.stary.backend.api.users.Role.ROLE_ADMIN;
    }
}
