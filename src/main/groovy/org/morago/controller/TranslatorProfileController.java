package org.morago.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.morago.dto.translatorprofile.TranslatorProfileRequest;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.service.TranslatorProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/translator-profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TranslatorProfileController {

    private final TranslatorProfileService translatorProfileService;

    @PostMapping
    public ResponseEntity<TranslatorProfileResponse> create(
            Authentication authentication,
            @RequestBody TranslatorProfileRequest request) {

        return ResponseEntity.ok(
                translatorProfileService.create(
                        authentication.getName(),
                        request
                )
        );
    }
}
