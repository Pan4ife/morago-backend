package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.admin.PendingTranslatorResponse;
import org.morago.dto.admin.RejectRequest;
import org.morago.dto.admin.UserPageResponse;
import org.morago.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Администрирование",
        description = "Управление пользователями и верификацией переводчиков. Все эндпоинты требуют роль ADMIN")
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Получить список всех пользователей",
            description = "Возвращает пагинированный список пользователей с их статусом и ролями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно возвращён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    @GetMapping("/users")
    public ResponseEntity<Page<UserPageResponse>> getAllUsers(
            Pageable pageable
    ) {

        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }
    @Operation(summary = "Получить анкеты переводчиков, ожидающие проверки",
            description = "Возвращает пагинированный список анкет переводчиков со статусом PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список анкет успешно возвращён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    @GetMapping("/translator-profiles/pending")
    public ResponseEntity<Page<PendingTranslatorResponse>> getPendingTranslators(
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getPendingTranslatorProfiles(pageable));

    }

    @Operation(summary = "Заблокировать пользователя",
            description = "Админ не может заблокировать самого себя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно заблокирован"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN, либо попытка заблокировать самого себя"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным id не найден")
    })
    @PatchMapping("/users/{id}/block")
    public ResponseEntity<String> block(@PathVariable Long id, Authentication authentication) {
        String adminEmail = authentication.getName();
        adminService.blockUser(id, adminEmail);
        return ResponseEntity.ok("User was blocked");
    }

    @Operation(summary = "Разблокировать пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно разблокирован"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным id не найден")
    })
    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<String> unblock(@PathVariable Long id, Authentication authentication) {
        String adminEmail = authentication.getName();
        adminService.unblockUser(id, adminEmail);
        return ResponseEntity.ok("User was unblocked");
    }
    @Operation(summary = "Одобрить анкету переводчика",
            description = "Присваивает пользователю роль TRANSLATOR. Допустимо только для анкет в статусе PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Анкета успешно одобрена"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Анкета с указанным id не найдена"),
            @ApiResponse(responseCode = "409", description = "Анкета не в статусе PENDING")
    })
    @PatchMapping("/translator-profiles/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable Long id, Authentication authentication) {
        String adminEmail = authentication.getName();
        adminService.approveTranslator(id, adminEmail);
        return ResponseEntity.ok("Translator approved");
    }

    @Operation(summary = "Отклонить анкету переводчика",
            description = "Если анкета была в статусе VERIFIED, роль TRANSLATOR у пользователя отзывается. Требует указания причины отклонения")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Анкета успешно отклонена"),
            @ApiResponse(responseCode = "400", description = "Не указана причина отклонения"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Анкета с указанным id не найдена"),
            @ApiResponse(responseCode = "409", description = "Анкета уже отклонена ранее")
    })
    @PatchMapping("/translator-profiles/{id}/reject")
    public ResponseEntity<String> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        adminService.rejectTranslator(id, request.reason(), adminEmail);
        return ResponseEntity.ok("Translator rejected");
    }
}
