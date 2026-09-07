package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.transaction.TopUpRequest;
import org.morago.dto.transaction.TransactionResponse;
import org.morago.model.Transaction;
import org.morago.service.TransactionService;
import org.morago.util.PaginationValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Транзакции",
        description = "Пополнение баланса и просмотр истории транзакций пользователя")
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Пополнить баланс",
            description = "Требуется роль USER. Сумма должна быть положительной")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Баланс успешно пополнен"),
            @ApiResponse(responseCode = "400", description = "Сумма не указана или не положительна"),
            @ApiResponse(responseCode = "403", description = "Требуется роль USER")
    })
    @PostMapping("/top-up")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> topUp(
            Authentication authentication,
            @Valid @RequestBody TopUpRequest request
            ) {
        Transaction transaction = transactionService.topUp(
                authentication.getName(),
                request.amount()
        );

        return ResponseEntity.ok(transactionService.toResponse(transaction));
    }
    @Operation(summary = "Получить историю своих транзакций",
            description = "Возвращает пагинированный список транзакций текущего пользователя, отсортированный по дате создания (сначала новые)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "История транзакций успешно возвращена")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationValidator.validate(page, size);
        Page<TransactionResponse> responses = transactionService
                .getMyTransactions(authentication.getName(), pageable)
                                .map(transactionService::toResponse);

        return ResponseEntity.ok(responses);
    }
}
