package org.morago.dto.withdrawal;

import org.morago.model.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalRequestResponse (
        Long id,
        BigDecimal amount,
        WithdrawalStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt
){}
