package org.morago.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest (

    @Email(message = "Wrong email format")
    @NotBlank(message = "Email cannot be empty")
    String email,

    @Size(min = 6, message = "Password must contain at least 6 characters")
    String password

) {}
