package com.stary.backend.api.reviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    void deleteByAuthorId(Long authorId);
    void deleteByTargetId(Long targetId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.target.id = :targetId")
    Double findAverageRatingByTargetId(@Param("targetId") Long targetId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.target.id = :targetId")
    Long countByTargetId(@Param("targetId") Long targetId);

    Optional<Review> findByAuthorIdAndTargetId(Long authorId, Long targetId);
}
