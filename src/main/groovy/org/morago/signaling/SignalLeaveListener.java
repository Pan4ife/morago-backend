package org.morago.signaling;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import org.morago.dto.signaling.SignalLeaveRequest;
import org.morago.exception.ForbiddenException;
import org.morago.service.CallService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SignalLeaveListener implements DataListener<SignalLeaveRequest> {
    private final CallAndUserForSignals callAndUserForSignals;
    private final CallService callService;
    private final SocketIOServer socketIOServer;

    public SignalLeaveListener(CallAndUserForSignals callAndUserForSignals,
                               CallService callService,
                               @Lazy SocketIOServer socketIOServer) {
        this.callAndUserForSignals = callAndUserForSignals;
        this.callService = callService;
        this.socketIOServer = socketIOServer;
    }

    @Override
    public void onData(SocketIOClient client, SignalLeaveRequest data, AckRequest ackSender) throws Exception {
        Long clientId = client.get("userId");
        if (clientId == null){
            client.disconnect();
            return;
        }
        Long callId =  data.callId();
        CallAndUser callAndUser = callAndUserForSignals.findCallAndUser(callId, clientId);
        try {
            callService.validateCallAccess(callAndUser.call(), callAndUser.user());
            client.leaveRoom("call-" + String.valueOf(callId));
            socketIOServer.getRoomOperations("call-" + String.valueOf(callId))
                    .sendEvent("signal:leave", client);
        } catch(ForbiddenException e) {
            client.sendEvent("signal:error", e.getMessage());
        }
    }
}