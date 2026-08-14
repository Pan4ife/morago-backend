package org.morago.dto.admin;


import java.util.Set;


public record PendingTranslatorResponse (
    Long id,

    String bio,

    Set<String> languages,

    Set<String> topics,

    String email
) {}
