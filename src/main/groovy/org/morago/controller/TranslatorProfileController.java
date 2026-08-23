package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.translatorprofile.HourlyRateRequest;
import org.morago.dto.translatorprofile.TranslatorProfileRequest;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.service.TranslatorProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/translator-profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRANSLATOR')")
public class TranslatorProfileController {

    private final TranslatorProfileService translatorProfileService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TranslatorProfileResponse> create(
            Authentication authentication,
            @Valid @RequestBody TranslatorProfileRequest request) {

        return ResponseEntity.ok(
                translatorProfileService.create(
                        authentication.getName(),
                        request
                )
        );
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorProfileResponse> getMyProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                translatorProfileService.getMyProfile(authentication)
        );
    }

    @PatchMapping("/hourly-rate")
    public ResponseEntity<TranslatorProfileResponse> updateHourlyRate(
            Authentication authentication,
            @Valid @RequestBody HourlyRateRequest request
            ) {
        return ResponseEntity.ok(
                translatorProfileService.updateHourlyRate(
                        authentication.getName(),
                        request.hourlyRate()
                )
        );
    }
}
