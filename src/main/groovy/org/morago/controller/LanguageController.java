package org.morago.controller;

import lombok.RequiredArgsConstructor;
import org.morago.dto.language.LanguageRequest;
import org.morago.dto.language.LanguageResponse;
import org.morago.service.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/languages")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    public ResponseEntity<List<LanguageResponse>> getAll() {

        return ResponseEntity.ok(languageService.getAll());
    }

    @PostMapping
    public ResponseEntity<LanguageResponse> create(
            @RequestBody LanguageRequest request) {

        return ResponseEntity.ok(languageService.create(request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        languageService.delete(id);

        return ResponseEntity.ok("Language deleted");
    }
}
