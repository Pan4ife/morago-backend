package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.language.LanguageResponse;
import org.morago.dto.translatorprofile.TranslatorProfileRequest;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.model.Language;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.repository.LanguageRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TranslatorProfileService {

    private final TranslatorProfileRepository translatorProfileRepository;

    private final UserRepository userRepository;

    private final LanguageRepository languageRepository;

    public TranslatorProfileResponse create(String email, TranslatorProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        TranslatorProfile profile = new TranslatorProfile();

        profile.setUser(user);

        profile.setBio(request.getBio());

        profile.setRating(0.0);

        profile.setOnline(false);

        Set<Language> languages = new HashSet<>(
                languageRepository.findAllById(request.getLanguageIds())
        );

        profile.setLanguages(languages);

        TranslatorProfile savedProfile = translatorProfileRepository.save(profile);

        return new TranslatorProfileResponse(
                savedProfile.getId(),
                user.getEmail(),
                savedProfile.getBio(),
                savedProfile.getRating(),
                savedProfile.isOnline()
        );
    }
}
