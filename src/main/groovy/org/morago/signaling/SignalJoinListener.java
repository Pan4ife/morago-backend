package org.morago.signaling;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import lombok.RequiredArgsConstructor;
import org.morago.dto.signaling.SignalJoinRequest;
import org.morago.exception.ForbiddenException;
import org.morago.service.CallService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignalJoinListener implements DataListener<SignalJoinRequest> {
    private final CallAndUserForSignals callAndUserForSignals;
    private final CallService callService;
    @Override
    public void onData(SocketIOClient client, SignalJoinRequest data, AckRequest ackSender) throws Exception {
        Long clientId = client.get("userId");
        if (clientId == null){
            client.disconnect();
            return;
        }
        Long callId =  data.callId();
        CallAndUser callAndUser = callAndUserForSignals.findCallAndUser(callId, clientId);
        try {
            callService.validateCallAccess(callAndUser.call(), callAndUser.user());
            client.joinRoom("call-" + String.valueOf(callId));
            client.sendEvent("signal:joined");
        } catch(ForbiddenException e) {
            client.sendEvent("signal:error", e.getMessage());
        }
    }
}