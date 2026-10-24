package org.morago.dto.auth;

import java.math.BigDecimal;
import java.util.Set;

public record MeResponse(
        Long id,
        String email,
        Set<String> roles,
        BigDecimal balance,
        Long translatorProfileId
) {}
