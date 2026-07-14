package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.review.ReviewRequest;
import org.morago.dto.review.ReviewResponse;
import org.morago.model.Review;
import org.morago.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public List<ReviewResponse> getAll() {

        return reviewRepository.findAll()
                .stream()
                .map(review ->
                        new ReviewResponse(
                                review.getId(),
                                review.getRating(),
                                review.getComment()
                        )
                )
                .toList();
    }

    public ReviewResponse create(ReviewRequest request) {

        Review review = new Review();

        review.setRating(request.getRating());

        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        return new ReviewResponse(savedReview.getId(), savedReview.getRating(), savedReview.getComment());
    }

    public void delete(Long id) {

        reviewRepository.deleteById(id);

    }

}
