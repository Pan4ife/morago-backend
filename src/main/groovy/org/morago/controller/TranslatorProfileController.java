package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Профиль переводчика",
        description = "Создание анкеты переводчика и управление собственным профилем")
@RestController
@RequestMapping("/translator-profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRANSLATOR')")
public class TranslatorProfileController {

    private final TranslatorProfileService translatorProfileService;

    @Operation(summary = "Создать анкету переводчика",
            description = "Требуется роль USER (переопределяет классовый TRANSLATOR только для этого метода). Анкета создаётся со статусом offline и рейтингом 0")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Анкета успешно создана"),
            @ApiResponse(responseCode = "400", description = "Не указана почасовая ставка или она не положительна"),
            @ApiResponse(responseCode = "403", description = "Требуется роль USER"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
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

    @Operation(summary = "Получить свой профиль переводчика",
            description = "Требуется роль USER или TRANSLATOR (переопределяет классовый TRANSLATOR для этого метода)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль успешно возвращён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль USER или TRANSLATOR"),
            @ApiResponse(responseCode = "404", description = "Пользователь или анкета переводчика не найдены")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorProfileResponse> getMyProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                translatorProfileService.getMyProfile(authentication)
        );
    }

    @Operation(summary = "Изменить почасовую ставку",
            description = "Требуется роль TRANSLATOR (наследуется с уровня класса, отдельной аннотации на методе нет). " +
                    "Известная проблема: при некорректной сумме сервис бросает InvalidAmountException, для которого нет отдельного обработчика — возвращается 500 вместо ожидаемого 400/409")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ставка успешно обновлена"),
            @ApiResponse(responseCode = "403", description = "Требуется роль TRANSLATOR"),
            @ApiResponse(responseCode = "404", description = "Пользователь или анкета переводчика не найдены"),
            @ApiResponse(responseCode = "500", description = "Некорректная сумма (см. описание метода — известная проблема обработки исключений)")
    })
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
