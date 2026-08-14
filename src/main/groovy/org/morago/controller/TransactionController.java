package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.transaction.TopUpRequest;
import org.morago.dto.transaction.TransactionResponse;
import org.morago.model.Transaction;
import org.morago.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/top-up")
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
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            Authentication authentication
    ) {
        List<TransactionResponse> responses = transactionService
                .getMyTransactions(authentication.getName())
                .stream()
                .map(transactionService::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
