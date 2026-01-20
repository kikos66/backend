package com.stary.backend.api.comments;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {
    private final CommentService svc;
    public CommentController(CommentService svc) { this.svc = svc; }

    @GetMapping("/products/{productId}/comments")
    public List<Comment> getComments(@PathVariable Long productId) {
        return svc.listForProduct(productId);
    }

    @PostMapping("/products/{productId}/comments")
    public ResponseEntity<?> postComment(@PathVariable Long productId, @RequestBody CommentDTO body) {
        Comment c = svc.create(productId, body.getContent());
        return ResponseEntity.status(201).body(c);
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<?> editComment(@PathVariable Long id, @RequestBody CommentDTO body) {
        Comment c = svc.edit(id, body.getContent());
        return ResponseEntity.ok(c);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }
}

class CommentDTO {
    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}