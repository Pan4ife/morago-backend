package org.morago.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpRequest (
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount
) {}
