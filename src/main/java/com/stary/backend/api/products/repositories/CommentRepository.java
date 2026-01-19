package com.stary.backend.api.products.repositories;

import com.stary.backend.api.products.Comment;
import com.stary.backend.api.products.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProductIdOrderByCreatedAtAsc(Long productId);
}