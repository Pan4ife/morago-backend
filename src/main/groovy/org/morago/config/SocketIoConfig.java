package org.morago.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.morago.dto.signaling.*;
import org.morago.signaling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


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
}
