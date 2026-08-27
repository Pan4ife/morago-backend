package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.language.LanguageRequest;
import org.morago.dto.language.LanguageResponse;
import org.morago.service.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Языки",
        description = "Справочник языков перевода. Просмотр доступен всем, изменение — только администратору")
@RestController
@RequestMapping("/languages")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    @Operation(summary = "Получить список всех языков",
            description = "Доступно без авторизации. Пагинация не применяется — список языков предполагается небольшим")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список языков успешно возвращён")
    })
    @GetMapping
    public ResponseEntity<List<LanguageResponse>> getAll() {

        return ResponseEntity.ok(languageService.getAll());
    }

    @Operation(summary = "Создать новый язык",
            description = "Требуется роль ADMIN. Известная проблема: поле name не имеет валидации, допускается пустое значение")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Язык успешно создан"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LanguageResponse> create(
            @Valid @RequestBody LanguageRequest request) {

        return ResponseEntity.ok(languageService.create(request)
        );
    }

    @Operation(summary = "Удалить язык",
            description = "Требуется роль ADMIN. Известная проблема: при несуществующем id возвращается 500 вместо 404 (нет проверки существования перед удалением)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Язык успешно удалён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "500", description = "Язык с указанным id не найден (см. описание метода)")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        languageService.delete(id);

        return ResponseEntity.ok("Language deleted");
    }
}
