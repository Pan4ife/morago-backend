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
        private final JwtService jwtService;
        private final UserRepository userRepository;

        @Override
        public AuthorizationResult getAuthorizationResult(HandshakeData handshakeData) {
            String header = handshakeData.getHttpHeaders().get("Authorization");
            if (header == null ||!header.startsWith("Bearer ")){
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            String token = header.substring(7);
            String username;

            try {
                username = jwtService.extractUsername(token);
            } catch (JwtException e) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            Optional<User> userOptional = userRepository.findByEmail(username);

            if(userOptional.isEmpty()){
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            User user = userOptional.get();

            if(user.getStatus() == UserStatus.BLOCKED){
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            if (!jwtService.extractTokenType(token).equals("access")) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            Map<String, Object> storeParams = Map.of("userId", user.getId());
            return new AuthorizationResult(true, storeParams);
        }
    }

