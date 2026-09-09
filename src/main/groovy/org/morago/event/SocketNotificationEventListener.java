package org.morago.event;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SocketNotificationEventListener {

    private final SocketIOServer socketIOServer;

    public SocketNotificationEventListener(@Lazy SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SocketNotificationEvent event) {
        socketIOServer.getRoomOperations(event.rooms().toArray(new String[0]))
                .sendEvent(event.eventName(), event.data());
    }
}