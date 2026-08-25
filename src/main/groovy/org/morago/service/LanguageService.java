package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.language.LanguageRequest;
import org.morago.dto.language.LanguageResponse;
import org.morago.model.Language;
import org.morago.repository.LanguageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final LanguageRepository languageRepository;

    public List<LanguageResponse> getAll() {
        return languageRepository.findAll()
                .stream()
                .map(language ->
                        new LanguageResponse(
                                language.getId(),
                                language.getName()
                        )
                )
                .toList();
    }

    public LanguageResponse create(LanguageRequest request) {

        Language language = new Language();

        language.setName(request.name());

        Language savedLanguage = languageRepository.save(language);

        return new LanguageResponse(savedLanguage.getId(), savedLanguage.getName());
    }

    public void delete(Long id) {

        languageRepository.deleteById(id);

    }
}
