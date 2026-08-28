package org.morago.service;

import lombok.RequiredArgsConstructor;

import org.morago.dto.transaction.TransactionResponse;
import org.morago.exception.*;
import org.morago.model.*;
import org.morago.repository.TransactionRepository;
import org.morago.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentNotificationService paymentNotificationService;

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public Transaction topUp(String email, BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }

        User user = getCurrentUser(email);

        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TOP_UP);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(now);
        transaction.setCompletedAt(now);
        Transaction topUpTransaction = transactionRepository.save(transaction);
        paymentNotificationService.notifyTopUp(topUpTransaction);
        return topUpTransaction;
    }

    public Page<Transaction> getMyTransactions(String email, Pageable pageable) {
        User user = getCurrentUser(email);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCall() != null ? transaction.getCall().getId() : null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }

    @Transactional
    public void payForCall(User client, User translator, BigDecimal amount, Call call) {

        if (transactionRepository.existsByCallIdAndType(call.getId(), TransactionType.CALL_CHARGE)) {
            throw new ConflictException("Call has already been charged");
        }

        if (client.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Недостаточно средств для звонка");
        }

        client.setBalance(client.getBalance().subtract(amount));
        translator.setBalance(translator.getBalance().add(amount));

        userRepository.save(client);
        userRepository.save(translator);

        LocalDateTime now = LocalDateTime.now();

        Transaction clientTransaction = new Transaction();
        clientTransaction.setUser(client);
        clientTransaction.setCall(call);
        clientTransaction.setAmount(amount.negate());
        clientTransaction.setType(TransactionType.CALL_CHARGE);
        clientTransaction.setStatus(TransactionStatus.COMPLETED);
        clientTransaction.setCreatedAt(now);
        clientTransaction.setCompletedAt(now);
        Transaction clientChargeTransaction = transactionRepository.save(clientTransaction);
        paymentNotificationService.notifyTransactionCharged(clientChargeTransaction);

        Transaction translatorTransaction = new Transaction();
        translatorTransaction.setUser(translator);
        translatorTransaction.setCall(call);
        translatorTransaction.setAmount(amount);
        translatorTransaction.setType(TransactionType.CALL_EARNING);
        translatorTransaction.setStatus(TransactionStatus.COMPLETED);
        translatorTransaction.setCreatedAt(now);
        translatorTransaction.setCompletedAt(now);
        Transaction transalatorEarnedTransaction = transactionRepository.save(translatorTransaction);
        paymentNotificationService.notifyTransactionEarned(transalatorEarnedTransaction);
    }
}
