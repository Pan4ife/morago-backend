package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.withdrawal.WithdrawalCreateRequest;
import org.morago.dto.withdrawal.WithdrawalRequestResponse;
import org.morago.service.WithdrawalRequestService;
import org.morago.util.PaginationValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Заявки на вывод средств",
        description = "Переводчики создают заявки на вывод заработанных средств, администратор их обрабатывает")
@RestController
@RequestMapping("/withdrawal-requests")
@RequiredArgsConstructor
public class WithdrawalRequestController {

    private final WithdrawalRequestService withdrawalRequestService;

    @Operation(summary = "Создать заявку на вывод средств",
            description = "Требуется роль TRANSLATOR. Сумма списывается с баланса сразу при создании заявки, до одобрения администратором")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка успешно создана"),
            @ApiResponse(responseCode = "400", description = "Сумма не указана или не положительна"),
            @ApiResponse(responseCode = "403", description = "Требуется роль TRANSLATOR"),
            @ApiResponse(responseCode = "409", description = "Недостаточно средств на балансе")
    })
    @PostMapping
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<WithdrawalRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody WithdrawalCreateRequest request
    ) {
        return ResponseEntity.ok(
                withdrawalRequestService.toResponse(
                        withdrawalRequestService.create(
                                authentication.getName(),
                                request.amount()
                        )
                )
        );
    }

    @Operation(summary = "Получить свои заявки на вывод",
            description = "Требуется роль TRANSLATOR. Возвращает пагинированный список заявок текущего переводчика")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заявок успешно возвращён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль TRANSLATOR")
    })
    @GetMapping("/my")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<Page<WithdrawalRequestResponse>> getMyRequests(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PaginationValidator.validate(page, size);
        Page<WithdrawalRequestResponse> responses = withdrawalRequestService
                .getMyRequests(authentication.getName(), pageable)
                .map(withdrawalRequestService::toResponse);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Получить заявки, ожидающие обработки",
            description = "Требуется роль ADMIN. Возвращает пагинированный список заявок в статусе PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заявок успешно возвращён"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WithdrawalRequestResponse>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationValidator.validate(page, size);
        Page<WithdrawalRequestResponse> responses = withdrawalRequestService
                .getPendingRequests(pageable)
                .map(withdrawalRequestService::toResponse);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Одобрить заявку на вывод",
            description = "Требуется роль ADMIN. Допустимо только для заявок в статусе PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка успешно одобрена"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Заявка с указанным id не найдена"),
            @ApiResponse(responseCode = "409", description = "Заявка не в статусе PENDING")
    })
    @PatchMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(
                withdrawalRequestService.toResponse(
                        withdrawalRequestService.approve(id)
                )
        );
    }

    @Operation(summary = "Отклонить заявку на вывод",
            description = "Требуется роль ADMIN. Списанная сумма возвращается на баланс переводчика. Допустимо только для заявок в статусе PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка успешно отклонена, средства возвращены"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Заявка с указанным id не найдена"),
            @ApiResponse(responseCode = "409", description = "Заявка не в статусе PENDING")
    })
    @PatchMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(
                withdrawalRequestService.toResponse(
                        withdrawalRequestService.reject(id)
                )
        );
    }

    @Operation(summary = "Отметить заявку как выплаченную",
            description = "Требуется роль ADMIN. Допустимо только для заявок в статусе APPROVED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка успешно отмечена как выплаченная"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Заявка с указанным id не найдена"),
            @ApiResponse(responseCode = "409", description = "Заявка не в статусе APPROVED")
    })
    @PatchMapping("/admin/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(
                withdrawalRequestService.toResponse(
                        withdrawalRequestService.markAsPaid(id)
                )
        );
    }
}
