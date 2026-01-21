package com.stary.backend.api.reviews;

import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReviewService {
    private final ReviewRepository repo;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public List<Review> listForUser(Long targetId) {
        return repo.findByTargetIdOrderByCreatedAtDesc(targetId);
    }

    @Transactional
    public Review createOrUpdate(Long targetId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        User author = getAuthenticatedUser();
        User target = userRepository.findById(targetId).orElseThrow(() -> new NoSuchElementException("Target user not found"));

        if (author.getId().equals(target.getId())) {
            throw new SecurityException("Cannot review yourself");
        }

        // Update if exists
        return repo.findByAuthorIdAndTargetId(author.getId(), target.getId())
                .map(existing -> {
                    existing.setRating(rating);
                    existing.setComment(comment);
                    existing.setUpdatedAt(java.time.Instant.now());
                    return repo.save(existing);
                })
                .orElseGet(() -> {
                    Review r = new Review();
                    r.setAuthor(author);
                    r.setTarget(target);
                    r.setRating(rating);
                    r.setComment(comment);
                    return repo.save(r);
                });
    }

    public RatingSummary getRatingSummary(Long targetId) {
        Double avg = repo.findAverageRatingByTargetId(targetId);
        Long count = repo.countByTargetId(targetId);
        if (avg == null) avg = 0.0;
        if (count == null) count = 0L;
        return new RatingSummary(avg, count);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    public static class RatingSummary {
        public final double average;
        public final long count;
        public RatingSummary(double average, long count) {
            this.average = average;
            this.count = count;
        }
    }
}
