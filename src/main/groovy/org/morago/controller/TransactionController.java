package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.transaction.TopUpRequest;
import org.morago.dto.transaction.TransactionResponse;
import org.morago.model.Transaction;
import org.morago.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

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

    @GetMapping("/me")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> responses = transactionService
                .getMyTransactions(authentication.getName(), pageable)
                                .map(transactionService::toResponse);

        return ResponseEntity.ok(responses);
    }
}
