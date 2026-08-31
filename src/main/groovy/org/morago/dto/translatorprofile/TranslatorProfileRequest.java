package org.morago.dto.translatorprofile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record TranslatorProfileRequest (

    String bio,

    Set<Long> languageIds,

    Set<Long> topicIds,

    @NotNull
    @DecimalMin(value = "0.01", message = "Hourly rate must be positive")
    BigDecimal hourlyRate

) {}
