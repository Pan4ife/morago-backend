package org.morago.dto.admin;

import org.morago.model.UserStatus;

import java.util.Set;

public record UserPageResponse(
        Long id,
        String email,
        UserStatus userStatus,
        Set<String> roles
)
{
}
