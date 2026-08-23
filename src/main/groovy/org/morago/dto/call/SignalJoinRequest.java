package org.morago.dto.call;


import jakarta.validation.constraints.NotNull;

public record SignalJoinRequest (
    @NotNull(message = "CallId cannot be empty")
    Long callId
){}
