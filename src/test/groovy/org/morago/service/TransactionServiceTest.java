package org.morago.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.exception.InsufficientBalanceException;
import org.morago.exception.InvalidAmountException;
import org.morago.model.Call;
import org.morago.model.Transaction;
import org.morago.model.TransactionType;
import org.morago.model.User;
import org.morago.repository.TransactionRepository;
import org.morago.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("client@test.com");
        user.setBalance(BigDecimal.valueOf(500));
    }

    @Test
    void topUp_shouldIncreaseBalance_whenAmountIsPositive() {

        when(userRepository.findByEmail("client@test.com"))
                .thenReturn(Optional.of(user));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.topUp("client@test.com", BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(700), user.getBalance());
        assertEquals(BigDecimal.valueOf(200), result.getAmount());

        verify(userRepository).save(user);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void topUp_shouldThrow_whenAmountIsZero() {

        assertThrows(InvalidAmountException.class, () ->
                transactionService.topUp("client@test.com", BigDecimal.ZERO)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void topUp_shouldThrow_whenAmountIsNegative() {

        assertThrows(InvalidAmountException.class, () ->
                transactionService.topUp("client@test.com", BigDecimal.valueOf(-50))
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void payForCall_shouldTransferMoneyFromClientToTranslator() {

        User client = new User();
        client.setId(1L);
        client.setBalance(BigDecimal.valueOf(1000));

        User translator = new User();
        translator.setId(2L);
        translator.setBalance(BigDecimal.valueOf(0));

        Call call = new Call();
        call.setId(10L);

        when(transactionRepository.existsByCallIdAndType(10L, TransactionType.CALL_CHARGE))
                .thenReturn(false);

        transactionService.payForCall(client, translator, BigDecimal.valueOf(300), call);

        assertEquals(BigDecimal.valueOf(700), client.getBalance());
        assertEquals(BigDecimal.valueOf(300), translator.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void payForCall_shouldThrow_whenClientHasInsufficientBalance() {

        User client = new User();
        client.setId(1L);
        client.setBalance(BigDecimal.valueOf(100));

        User translator = new User();
        translator.setId(2L);
        translator.setBalance(BigDecimal.ZERO);

        Call call = new Call();
        call.setId(10L);

        when(transactionRepository.existsByCallIdAndType(10L, TransactionType.CALL_CHARGE))
                .thenReturn(false);

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.payForCall(client, translator, BigDecimal.valueOf(500), call)
        );

        // баланс не должен был измениться
        assertEquals(BigDecimal.valueOf(100), client.getBalance());
        assertEquals(BigDecimal.ZERO, translator.getBalance());

        verify(userRepository, never()).save(any(User.class));
    }
}
