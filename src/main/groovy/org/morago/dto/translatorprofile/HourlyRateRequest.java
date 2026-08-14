package org.morago.dto.translatorprofile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record HourlyRateRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Hpurly rate must be positive")
        BigDecimal hourlyRate
){
}
