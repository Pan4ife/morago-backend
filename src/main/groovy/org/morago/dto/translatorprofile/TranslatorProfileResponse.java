package org.morago.dto.translatorprofile;

import java.math.BigDecimal;

public record TranslatorProfileResponse (

    Long id,

    String email,

    String bio,

    Double rating,

    boolean online,

    BigDecimal hourlyRate

) {}
