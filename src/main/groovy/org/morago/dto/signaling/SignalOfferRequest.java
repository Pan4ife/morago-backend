package org.morago.dto.signaling;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignalOfferRequest(
        @NotNull
        Long callId,
        @NotNull
        @Size(max = 50)
        String spdMessage
)
{}
