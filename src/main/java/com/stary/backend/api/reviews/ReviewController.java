package com.stary.backend.api.reviews;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class ReviewController {
    private final ReviewService svc;

    public ReviewController(ReviewService svc) {
        this.svc = svc;
    }

    @GetMapping("/{id}/reviews")
    public List<Review> getReviews(@PathVariable Long id) {
        return svc.listForUser(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> postReview(@PathVariable Long id, @RequestBody ReviewDTO body) {
        Review r = svc.createOrUpdate(id, body.getRating(), body.getComment());
        return ResponseEntity.status(201).body(r);
    }

    @GetMapping("/{id}/rating")
    public ResponseEntity<?> getRating(@PathVariable Long id) {
        ReviewService.RatingSummary s = svc.getRatingSummary(id);
        return ResponseEntity.ok(Map.of("average", s.average, "count", s.count));
    }
}

class ReviewDTO {
    private Integer rating;
    private String comment;
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
}
