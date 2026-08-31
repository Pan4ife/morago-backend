package org.morago.dto.withdrawal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalCreateRequest (
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount
){}
