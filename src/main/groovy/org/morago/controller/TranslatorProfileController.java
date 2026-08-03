package org.morago.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.translatorprofile.TranslatorProfileRequest;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.service.TranslatorProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/translator-profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TranslatorProfileController {

    private final TranslatorProfileService translatorProfileService;

    @PostMapping
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
    public ResponseEntity<TranslatorProfileResponse> getMyProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                translatorProfileService.getMyProfile(authentication)
        );
    }
}
