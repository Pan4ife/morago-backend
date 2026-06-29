package org.morago.dto.auth;

public record JwtResponse (

    String accessToken,

    String refreshToken

) {}
