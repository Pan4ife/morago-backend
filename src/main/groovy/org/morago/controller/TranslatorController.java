package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.service.TranslatorProfileService;
import org.morago.util.PaginationValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Переводчики", description = "Каталог проверенных переводчиков для выбора и звонка")
@RestController
@RequestMapping("/translators")
@RequiredArgsConstructor
public class TranslatorController {

    private final TranslatorProfileService translatorProfileService;

    @Operation(summary = "Список проверенных переводчиков",
            description = "Доступно любому вошедшему пользователю. Только анкеты со статусом VERIFIED. " +
                    "id в ответе — это translatorId для POST /calls. Поддерживает пагинацию (page, size)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список успешно возвращён"),
            @ApiResponse(responseCode = "401", description = "Не выполнен вход")
    })
    @GetMapping
    public ResponseEntity<Page<TranslatorProfileResponse>> getVerified(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationValidator.validate(page, size);
        return ResponseEntity.ok(translatorProfileService.getVerified(pageable));
    }
}
