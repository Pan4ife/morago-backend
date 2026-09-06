package org.morago.dto.signaling;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignalAnswerRequest(
        @NotNull
        Long callId,
        @NotNull
        @Size(max = 15000)
        String sdpMessage
)
{}

