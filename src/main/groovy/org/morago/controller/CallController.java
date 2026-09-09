package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.service.CallService;
import org.morago.util.PaginationValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Управление звонками",
        description =
                "Создание звонков и управление их жизненным циклом: назначение переводчика, старт, завершение, отмена")
@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @Operation(summary = "Получить список звонков",
            description = "Админ видит все звонки, переводчик — только свои как переводчик, клиент — только свои как клиент. Поддерживает пагинацию (page, size)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список звонков успешно возвращён")
    })
    @GetMapping
    public ResponseEntity<Page<CallResponse>> getAll(Authentication authentication,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size)
    {
        Pageable pageable = PaginationValidator.validate(page, size);
        return ResponseEntity.ok(callService.getAll(authentication.getName(), pageable));

    }
    @Operation(summary = "Получение информации о звонке по ID",
            description = "Доступно клиенту и переводчику, участвующим в звонке, а также администратору." +
                    " Для остальных пользователей возвращает 403")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Звонок найден и возвращён"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён — пользователь не участник звонка и не админ"),
            @ApiResponse(responseCode = "404", description = "Звонок с указанным id не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CallResponse> getById(@PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(callService.getById(id, authentication.getName()));
    }

    @Operation(summary = "Создать звонок",
            description = "Требуется роль USER. Создаёт звонок с указанным переводчиком, если тот сейчас online")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Звонок успешно создан"),
            @ApiResponse(responseCode = "403", description = "Требуется роль USER"),
            @ApiResponse(responseCode = "404", description = "Переводчик с указанным id не найден"),
            @ApiResponse(responseCode = "409", description = "Переводчик сейчас недоступен (не online)")
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CallResponse> create(Authentication authentication,
                                               @Valid @RequestBody CallRequest request) {

        return ResponseEntity.ok(
                callService.create(
                        authentication.getName(),
                        request
                )
        );

    }

    @Operation(summary = "Удалить звонок",
            description = "Требуется роль ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Звонок успешно удалён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Звонок с указанным id не найден")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        callService.delete(
                        id,
                        authentication.getName()
        );

        return ResponseEntity.ok("Call deleted");

    }

    @Operation(summary = "Завершить звонок",
            description = "Требуется роль TRANSLATOR (участник звонка) или ADMIN. Допустимо только из статуса IN_PROGRESS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Звонок успешно завершён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль TRANSLATOR/ADMIN, либо пользователь не участник звонка"),
            @ApiResponse(responseCode = "404", description = "Звонок с указанным id не найден"),
            @ApiResponse(responseCode = "409", description = "Недопустимый переход статуса звонка")
    })
    @PatchMapping("/{id}/finish")
    @PreAuthorize("hasRole('TRANSLATOR') or hasRole('ADMIN')")
    public ResponseEntity<CallResponse> finish(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.finish(
                id,
                authentication.getName())
        );
    }

    @Operation(summary = "Отменить звонок",
            description = "Требуется роль USER (клиент звонка) или ADMIN. Допустимо только из статуса CREATED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Звонок успешно отменён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль USER/ADMIN, либо пользователь не клиент звонка"),
            @ApiResponse(responseCode = "404", description = "Звонок с указанным id не найден"),
            @ApiResponse(responseCode = "409", description = "Недопустимый переход статуса звонка")
    })
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CallResponse> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.cancel(
                id,
                authentication.getName())
        );
    }
    @Operation(summary = "Начать звонок",
            description = "Требуется роль TRANSLATOR (участник звонка) или ADMIN. Допустимо только из статуса CREATED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Звонок успешно начат"),
            @ApiResponse(responseCode = "403", description = "Требуется роль TRANSLATOR/ADMIN, либо пользователь не участник звонка"),
            @ApiResponse(responseCode = "404", description = "Звонок с указанным id не найден"),
            @ApiResponse(responseCode = "409", description = "Недопустимый переход статуса звонка")
    })
    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('TRANSLATOR') or hasRole('ADMIN')")
    public ResponseEntity<CallResponse> start(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.start(
                id,
                authentication.getName())
        );
    }

}
