package org.morago.dto.signaling;


import jakarta.validation.constraints.NotNull;

public record SignalJoinRequest (
    @NotNull(message = "CallId cannot be empty")
    Long callId
){}
