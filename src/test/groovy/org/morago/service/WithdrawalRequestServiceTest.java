package org.morago.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.exception.ConflictException;
import org.morago.exception.InsufficientBalanceException;
import org.morago.model.Transaction;
import org.morago.model.User;
import org.morago.model.WithdrawalRequest;
import org.morago.model.WithdrawalStatus;
import org.morago.repository.TransactionRepository;
import org.morago.repository.UserRepository;
import org.morago.repository.WithdrawalRequestRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WithdrawalRequestServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WithdrawalRequestService withdrawalRequestService;

    private User translator;

    @BeforeEach
    void setUp() {
        translator = new User();
        translator.setId(1L);
        translator.setEmail("translator@test.com");
        translator.setBalance(BigDecimal.valueOf(200));
    }

    @Test
    void create_shouldThrow_whenAmountExceedsBalance() {

        when(userRepository.findByEmail("translator@test.com"))
                .thenReturn(Optional.of(translator));

        assertThrows(InsufficientBalanceException.class, () ->
                withdrawalRequestService.create("translator@test.com", BigDecimal.valueOf(500))
        );

        assertEquals(BigDecimal.valueOf(200), translator.getBalance());

        verify(withdrawalRequestRepository, never()).save(any(WithdrawalRequest.class));
    }

    @Test
    void create_shouldReserveFunds_whenAmountIsValid() {

        when(userRepository.findByEmail("translator@test.com"))
                .thenReturn(Optional.of(translator));

        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest result = withdrawalRequestService.create("translator@test.com", BigDecimal.valueOf(150));

        assertEquals(BigDecimal.valueOf(50), translator.getBalance());
        assertEquals(WithdrawalStatus.PENDING, result.getStatus());

        verify(userRepository).save(translator);
    }

    @Test
    void approve_shouldChangeStatusToApproved() {

        WithdrawalRequest request = new WithdrawalRequest();
        request.setId(1L);
        request.setStatus(WithdrawalStatus.PENDING);
        request.setAmount(BigDecimal.valueOf(100));

        when(withdrawalRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest result = withdrawalRequestService.approve(1L);

        assertEquals(WithdrawalStatus.APPROVED, result.getStatus());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void approve_shouldThrow_whenRequestIsNotPending() {

        WithdrawalRequest request = new WithdrawalRequest();
        request.setId(1L);
        request.setStatus(WithdrawalStatus.APPROVED);

        when(withdrawalRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () ->
                withdrawalRequestService.approve(1L)
        );
    }

    @Test
    void reject_shouldRefundBalance() {

        WithdrawalRequest request = new WithdrawalRequest();
        request.setId(1L);
        request.setStatus(WithdrawalStatus.PENDING);
        request.setAmount(BigDecimal.valueOf(150));
        request.setTranslator(translator);


        translator.setBalance(BigDecimal.valueOf(50));

        when(withdrawalRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest result = withdrawalRequestService.reject(1L);

        assertEquals(WithdrawalStatus.REJECTED, result.getStatus());
        assertEquals(BigDecimal.valueOf(200), translator.getBalance()); // деньги вернулись

        verify(userRepository).save(translator);
    }
}
