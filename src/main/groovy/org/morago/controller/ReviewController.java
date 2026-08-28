package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.review.ReviewRequest;
import org.morago.dto.review.ReviewResponse;
import org.morago.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAll(pageable));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            Authentication authentication,
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(reviewService.create(authentication.getName(), request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        reviewService.delete(id, authentication.getName());

        return ResponseEntity.ok("Review deleted");
    }
}
