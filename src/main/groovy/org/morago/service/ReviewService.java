package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.review.ReviewRequest;
import org.morago.dto.review.ReviewResponse;
import org.morago.exception.ForbiddenException;
import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.CallRepository;
import org.morago.repository.ReviewRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private boolean isAdmin(User user) {
        return user.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private void validateReviewAccess(Review review, User user) {

        if (!isAdmin(user)
                && !review.getCall()
                .getClient()
                .getId()
                .equals(user.getId())) {

            throw new ForbiddenException("Access denied");
        }
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

        Call call = callRepository.findById(request.callId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);

        if (!call.getClient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied");
        }

        if (call.getStatus() != CallStatus.FINISHED) {
            throw new ConflictException("Call is not finished");
        }

        if (call.getReview() != null) {
            throw new ConflictException("Review already exists");
        }

        Review review = new Review();

        review.setCall(call);

        review.setRating(request.rating());

        review.setComment(request.comment());

        Review savedReview = reviewRepository.save(review);



        Double avg = reviewRepository.findAverageRatingByTranslatorId(call.getTranslator().getId());

        TranslatorProfile translator = call.getTranslator();

        translator.setRating(avg == null ? 0.0 : avg);

        translatorProfileRepository.save(translator);

        return new ReviewResponse(savedReview.getId(), savedReview.getRating(), savedReview.getComment());
    }

    @Transactional
    public void delete(Long id, String email) {

        User currentUser = getCurrentUser(email);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review not found"));

        validateReviewAccess(review, currentUser);

        TranslatorProfile translator = review.getCall().getTranslator();

        Call call = review.getCall();
        call.setReview(null);
        review.setCall(null);

        reviewRepository.delete(review);

        Double avg = reviewRepository.findAverageRatingByTranslatorId(translator.getId());

        translator.setRating(avg == null ? 0.0 : avg);

        translatorProfileRepository.save(translator);

    }

}
