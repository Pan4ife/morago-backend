package org.morago.config;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.morago.dto.signaling.*;
import org.morago.model.User;
import org.morago.model.UserStatus;
import org.morago.repository.UserRepository;
import org.morago.service.JwtService;
import org.morago.signaling.*;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Configuration
public class SocketIoConfig {
    @Value("${socketio.host}")
    private String host;
    @Value("${socketio.port}")
    private int port;
    @Value("${socketio.allowed-origins}")
    private String allowedOrigins;
    private final SignalJoinListener signalJoinListener;
    private final JwtAuthorizationListener jwtAuthorizationListener;
    private final RoomForClient roomForClient;
    private static final Logger log = LoggerFactory.getLogger(SocketIoConfig.class);


    @Bean
    public SocketIOServer socketIOServer(SignalAnswerListener signalAnswerListener,
                                         SignalOfferListener signalOfferListener,
                                         SignalIceCandidateListener signalIceCandidateListener,
                                         SignalLeaveListener signalLeaveListener) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin(allowedOrigins);
        config.setAuthorizationListener(jwtAuthorizationListener);
        SocketIOServer socketIOServer = new SocketIOServer(config);
        socketIOServer.addConnectListener(roomForClient);
        socketIOServer.addEventListener("signal:join", SignalJoinRequest.class, signalJoinListener);
        socketIOServer.addEventListener("signal:answer", SignalAnswerRequest.class, signalAnswerListener);
        socketIOServer.addEventListener("signal:offer", SignalOfferRequest.class, signalOfferListener);
        socketIOServer.addEventListener("signal:ice-candidate", SignalIceCandidateRequest.class, signalIceCandidateListener);
        socketIOServer.addEventListener("signal:leave", SignalLeaveRequest.class, signalLeaveListener);
        return socketIOServer;
    }

    @Component
    @RequiredArgsConstructor
    static class SocketIOServerLifecycle {

        private final SocketIOServer socketIOServer;

        @Bean
        CommandLineRunner startSocketIOServer() {
            return args -> {
                try {
                    socketIOServer.start();
                    log.info("SocketIoServer is launched");
                } catch (RuntimeException e) {
                    log.error("SocketIoServer is unavailable", e);
                }
            };
        }

        @PreDestroy
        public void stopSocketIOServer() {
            try {
                socketIOServer.stop();
                log.info("SocketIO server stopped");
            } catch (RuntimeException e) {
                log.error("Error stopping SocketIO server", e);
            }
        }
    }

    @Component
    @RequiredArgsConstructor
    static class JwtAuthorizationListener implements AuthorizationListener {
        private final JwtService jwtService;
        private final UserRepository userRepository;

        @Override
        public AuthorizationResult getAuthorizationResult(HandshakeData handshakeData) {
            String token = handshakeData.getSingleUrlParam("token");
            String username;
            if (token == null) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            try {
                username = jwtService.extractUsername(token);
            } catch (JwtException e) {
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

            if (!jwtService.extractTokenType(token).equals("access")) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            Map<String, Object> storeParams = Map.of("userId", user.getId());
            return new AuthorizationResult(true, storeParams);
        }
    }

    @Component
    @RequiredArgsConstructor
    static class RoomForClient implements ConnectListener {
        @Override
        public void onConnect(SocketIOClient client) {
            Long id = client.get("userId");

            if (id == null) {
                client.disconnect();
                return;
            }

            String room = String.valueOf(id);
            client.joinRoom(room);
        }
    }
}
