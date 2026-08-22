package org.morago.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest (
    @NotBlank(message = "Reason cannot be empty")
    String reason
)
{}
