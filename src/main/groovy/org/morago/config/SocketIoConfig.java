package org.morago.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class SocketIoConfig {
    @Bean
    public SocketIOServer socketIOServer(){
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(9092);
        return new SocketIOServer(config);
    }

    @Component
    @RequiredArgsConstructor
    static class SocketIOServerLifecycle{
        private final SocketIOServer socketIOServer;

        @Bean
        CommandLineRunner startSocketIOServer(){
            return args -> socketIOServer.start();
        }

        @PreDestroy
        public void stopSocketIOServer(){
            socketIOServer.stop();
        }
    }
}
