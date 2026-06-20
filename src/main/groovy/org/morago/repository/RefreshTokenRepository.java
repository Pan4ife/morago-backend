package org.morago.repository;

import org.morago.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByToken(String token);
}
