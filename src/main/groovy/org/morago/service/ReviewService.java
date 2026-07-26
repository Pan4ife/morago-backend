package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.review.ReviewRequest;
import org.morago.dto.review.ReviewResponse;
import org.morago.exception.AccessDeniedException;
import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.CallRepository;
import org.morago.repository.ReviewRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final CallRepository callRepository;

    private final UserRepository userRepository;

    private final TranslatorProfileRepository translatorProfileRepository;


    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }



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

    @Transactional
    public ReviewResponse create(String email, ReviewRequest request) {

        Call call = callRepository.findById(request.getCallId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);

        if (!call.getClient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (call.getStatus() != CallStatus.FINISHED) {
            throw new ConflictException("Call is not finished");
        }

        if (call.getReview() != null) {
            throw new ConflictException("Review already exists");
        }

        Review review = new Review();

        review.setCall(call);

        review.setRating(request.getRating());

        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);



        Double avg = reviewRepository.findAverageRatingByTranslatorId(call.getTranslator().getId());

        TranslatorProfile translator = call.getTranslator();

        translator.setRating(avg == null ? 0.0 : avg);

        translatorProfileRepository.save(translator);

        return new ReviewResponse(savedReview.getId(), savedReview.getRating(), savedReview.getComment());
    }

    public void delete(Long id) {

        reviewRepository.deleteById(id);

    }

}
