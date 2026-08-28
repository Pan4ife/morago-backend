package org.morago.dto.signaling;

import jakarta.validation.constraints.NotNull;

public record SignalLeaveRequest(
        @NotNull(message = "CallId cannot be empty")
        Long callId
) {
}
