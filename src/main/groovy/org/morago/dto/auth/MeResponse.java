package org.morago.dto.auth;

import java.math.BigDecimal;
import java.util.Set;

/** Ответ GET /auth/me: кто вошёл — id, email, роли, баланс и id анкеты переводчика (null, если анкеты нет). */
public record MeResponse(
        Long id,
        String email,
        Set<String> roles,
        BigDecimal balance,
        Long translatorProfileId
) {}
