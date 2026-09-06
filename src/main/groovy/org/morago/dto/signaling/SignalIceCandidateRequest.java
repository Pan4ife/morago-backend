package org.morago.dto.signaling;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignalIceCandidateRequest(
        @NotNull
        Long callId,
        @NotNull
        @Size(max = 900)
        String iceCandidate
)
{}


