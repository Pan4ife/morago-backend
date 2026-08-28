package org.morago.service;

import com.corundumstudio.socketio.SocketIOServer;
import org.morago.model.Call;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class CallNotificationService {

    private final SocketIOServer socketIOServer;
    public CallNotificationService(@Lazy SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
    }

    public void notifyIncomingCall(Call call){
        Long translatorId = call.getTranslator().getUser().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("call:incoming", call.getId());
    }
    public void notifyCallStarted(Call call){
        Long translatorId = call.getTranslator().getUser().getId();
        Long clientId = call.getClient().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId), String.valueOf(clientId))
                .sendEvent("call:started", call.getId());
    }
    public void notifyCallFinished(Call call){
        Long translatorId = call.getTranslator().getUser().getId();
        Long clientId = call.getClient().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId), String.valueOf(clientId))
                .sendEvent("call:finished", call.getId());
    }

    public void notifyCallCancelled(Call call){
        Long translatorId = call.getTranslator().getUser().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("call:canceled", call.getId());
    }
}
