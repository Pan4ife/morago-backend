package org.morago.dto.transaction;

import jakarta.persistence.*;


import org.morago.model.TransactionStatus;
import org.morago.model.TransactionType;


import java.math.BigDecimal;
import java.time.LocalDateTime;


public record TransactionResponse (

    Long id,
    Long callId,
    BigDecimal amount,
    TransactionType type,
    TransactionStatus status,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {

        }
