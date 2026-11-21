package org.morago.config;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.morago.model.User;
import org.morago.model.UserStatus;
import org.morago.repository.UserRepository;
import org.morago.service.JwtService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;


    @Component
    @RequiredArgsConstructor
    public class JwtAuthorizationListener implements AuthorizationListener {
        private static final String BEARER_PREFIX = "Bearer ";

        private final JwtService jwtService;
        private final UserRepository userRepository;

        @Override
        public AuthorizationResult getAuthorizationResult(HandshakeData handshakeData) {
            String token = extractToken(handshakeData);
            if (token == null) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            return authorize(token);
        }

        /** Заголовок Authorization: Bearer главнее параметра адреса ?token=. */
        private String extractToken(HandshakeData handshakeData) {
            String header = handshakeData.getHttpHeaders().get("Authorization");
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                return header.substring(BEARER_PREFIX.length());
            }
            String param = handshakeData.getSingleUrlParam("token");
            return param == null || param.isBlank() ? null : param;
        }

        private AuthorizationResult authorize(String token) {
            String username;
            String tokenType;
            try {
                username = jwtService.extractUsername(token);
                tokenType = jwtService.extractTokenType(token);
            } catch (JwtException e) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            if (!"access".equals(tokenType)) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            Optional<User> userOptional = userRepository.findByEmail(username);
            if (userOptional.isEmpty()) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            User user = userOptional.get();

            if (user.getStatus() == UserStatus.BLOCKED) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            Map<String, Object> storeParams = Map.of("userId", user.getId());
            return new AuthorizationResult(true, storeParams);
        }
    }

