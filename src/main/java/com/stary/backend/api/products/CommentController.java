package com.stary.backend.api.products;

import com.stary.backend.api.products.repositories.CommentRepository;
import com.stary.backend.api.products.repositories.ProductRepository;
import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CommentController(CommentRepository commentRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/products/{id}/comments")
    public List<Comment> getComments(@PathVariable Long id) {
        return commentRepository.findByProductIdOrderByCreatedAtAsc(id);
    }

    @PostMapping("/products/{id}/comments")
    public Comment addComment(@PathVariable Long id, @RequestBody Comment newComment) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = getAuthenticatedUser();
        newComment.setProduct(product);
        newComment.setUser(user);
        return commentRepository.save(newComment);
    }

    @PutMapping("/comments/{id}")
    public Comment editComment(@PathVariable Long id, @RequestBody Comment updated) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User user = getAuthenticatedUser();
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not owner of comment");
        }

        comment.setContent(updated.getContent());
        return commentRepository.save(comment);
    }

    @DeleteMapping("/comments/{id}")
    @Transactional
    public void deleteComment(@PathVariable Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User user = getAuthenticatedUser();
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not owner of comment");
        }

        commentRepository.delete(comment);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}