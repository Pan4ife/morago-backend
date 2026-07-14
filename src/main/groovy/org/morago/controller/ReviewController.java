package org.morago.controller;

import lombok.RequiredArgsConstructor;
import org.morago.dto.review.ReviewRequest;
import org.morago.dto.review.ReviewResponse;
import org.morago.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAll() {

        return ResponseEntity.ok(reviewService.getAll());
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(reviewService.create(request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        reviewService.delete(id);

        return ResponseEntity.ok("Review deleted");
    }
}
