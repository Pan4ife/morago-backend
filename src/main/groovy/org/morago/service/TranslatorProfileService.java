package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.translatorprofile.TranslatorProfileRequest;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.exception.InvalidAmountException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.Language;
import org.morago.model.Topic;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.repository.LanguageRepository;
import org.morago.repository.TopicRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TranslatorProfileService {

    private final TranslatorProfileRepository translatorProfileRepository;

    private final UserRepository userRepository;

    private final LanguageRepository languageRepository;

    private final TopicRepository topicRepository;

    public TranslatorProfileResponse create(String email, TranslatorProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException("User not found")
                );

        TranslatorProfile profile = new TranslatorProfile();

        profile.setUser(user);

        profile.setBio(request.bio());

        profile.setRating(0.0);

        profile.setOnline(false);

        Set<Language> languages = new HashSet<>(
                languageRepository.findAllById(request.languageIds())
        );

        profile.setLanguages(languages);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setHourlyRate(request.hourlyRate());
        Set<Topic> topics = new HashSet<>( topicRepository.findAllById(request.topicIds()));
        profile.setTopics(topics);


        TranslatorProfile savedProfile = translatorProfileRepository.save(profile);

        return new TranslatorProfileResponse(
                savedProfile.getId(),
                user.getEmail(),
                savedProfile.getBio(),
                savedProfile.getRating(),
                savedProfile.isOnline(),
                savedProfile.getHourlyRate()
        );
    }

    public TranslatorProfileResponse getMyProfile(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TranslatorProfile profile =
                translatorProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Profile not found"));

        return new TranslatorProfileResponse(
                profile.getId(),
                user.getEmail(),
                profile.getBio(),
                profile.getRating(),
                profile.isOnline(),
                profile.getHourlyRate()
        );
    }

    @Transactional
    public TranslatorProfileResponse updateHourlyRate(String email, BigDecimal newRate) {

        if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Hourly rate must be positive");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TranslatorProfile profile = translatorProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.setHourlyRate(newRate);
        profile.setUpdatedAt(LocalDateTime.now());

        TranslatorProfile saved = translatorProfileRepository.save(profile);

        return new TranslatorProfileResponse(
                saved.getId(),
                user.getEmail(),
                saved.getBio(),
                saved.getRating(),
                saved.isOnline(),
                saved.getHourlyRate()
        );
    }
}
