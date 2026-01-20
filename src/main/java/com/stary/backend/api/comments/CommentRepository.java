package com.stary.backend.api.comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProductIdOrderByCreatedAtAsc(Long productId);

    Page<Comment> findByProductIdOrderByCreatedAtAsc(Long productId, Pageable pageable);

    void deleteByAuthorId(Long authorId);
    void deleteByProductId(Long productId);
}