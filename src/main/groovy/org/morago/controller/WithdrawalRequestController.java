package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.withdrawal.WithdrawalCreateRequest;
import org.morago.dto.withdrawal.WithdrawalRequestResponse;
import org.morago.service.WithdrawalRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/withdrawal-requests")
@RequiredArgsConstructor
public class WithdrawalRequestController {

    private final WithdrawalRequestService withdrawalRequestService;

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

    @GetMapping("/my")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<Page<WithdrawalRequestResponse>> getMyRequests(
            Authentication authentication,
            Pageable pageable
    ) {
        Page<WithdrawalRequestResponse> responses = withdrawalRequestService
                .getMyRequests(authentication.getName(), pageable)
                .map(withdrawalRequestService::toResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WithdrawalRequestResponse>> getPendingRequests(
            Pageable pageable
    ) {
        Page<WithdrawalRequestResponse> responses = withdrawalRequestService
                .getPendingRequests(pageable)
                .map(withdrawalRequestService::toResponse);

        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(
                withdrawalRequestService.toResponse(
                        withdrawalRequestService.approve(id)
                )
        );
    }

    @PatchMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(
                withdrawalRequestService.toResponse(
                        withdrawalRequestService.reject(id)
                )
        );
    }

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
