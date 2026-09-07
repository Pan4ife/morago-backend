package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.withdrawal.WithdrawalRequestResponse;
import org.morago.exception.ConflictException;
import org.morago.exception.InsufficientBalanceException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.TransactionRepository;
import org.morago.repository.UserRepository;
import org.morago.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class    WithdrawalRequestService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public WithdrawalRequest create(String email, BigDecimal amount) {

        User translator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (translator.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Недостаточно средств для вывода");
        }

        translator.setBalance(translator.getBalance().subtract(amount));
        userRepository.save(translator);

        WithdrawalRequest request = new WithdrawalRequest();
        request.setTranslator(translator);
        request.setAmount(amount);
        request.setStatus(WithdrawalStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        WithdrawalRequest savedRequest = withdrawalRequestRepository.save(request);

        Transaction transaction = new Transaction();
        transaction.setUser(translator);
        transaction.setAmount(amount.negate());
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);

        savedRequest.setTransactionId(savedTransaction.getId());
        return withdrawalRequestRepository.save(savedRequest);
    }

    public List<WithdrawalRequest> getMyRequests(String email) {
        User translator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return withdrawalRequestRepository.findByTranslatorIdOrderByCreatedAtDesc(translator.getId());
    }

    public List<WithdrawalRequest> getPendingRequests() {
        return withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(WithdrawalStatus.PENDING);
    }

    @Transactional
    public WithdrawalRequest approve(Long requestId) {

        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal request not found"));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            throw new ConflictException("Only pending request can be approved");
        }

        request.setStatus(WithdrawalStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());

        return withdrawalRequestRepository.save(request);
    }

    @Transactional
    public WithdrawalRequest reject(Long requestId) {

        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal request not found"));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            throw new ConflictException("Only pending requests can be rejected");
        }

        User translator = request.getTranslator();
        translator.setBalance(translator.getBalance().add(request.getAmount()));
        userRepository.save(translator);

        request.setStatus(WithdrawalStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());

        markTransactionAs(request.getTransactionId(), TransactionStatus.FAILED);

        return withdrawalRequestRepository.save(request);
    }

    @Transactional
    public WithdrawalRequest markAsPaid(Long requestId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal request not found"));

        if (request.getStatus() != WithdrawalStatus.APPROVED) {
            throw new ConflictException("Only approved requests can be marked as paid");
        }

        request.setStatus(WithdrawalStatus.PAID);
        request.setProcessedAt(LocalDateTime.now());

        markTransactionAs(request.getTransactionId(), TransactionStatus.COMPLETED);

        return withdrawalRequestRepository.save(request);
    }

    private void markTransactionAs(Long transactionId, TransactionStatus status) {
        if (transactionId == null) {
            return;
        }
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            transaction.setStatus(status);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        });
    }

    public WithdrawalRequestResponse toResponse(WithdrawalRequest request) {
        return new WithdrawalRequestResponse(
                request.getId(),
                request.getAmount(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getProcessedAt()
        );
    }
}
