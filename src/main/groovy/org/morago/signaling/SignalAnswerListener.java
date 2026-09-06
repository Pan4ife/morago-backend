package org.morago.signaling;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import jakarta.validation.Validator;
import org.morago.dto.signaling.SignalAnswerRequest;
import org.morago.exception.ForbiddenException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.service.CallService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SignalAnswerListener implements DataListener<SignalAnswerRequest> {
    private final CallAndUserForSignals callAndUserForSignals;
    private final CallService callService;
    private final SocketIOServer socketIOServer;

    public SignalAnswerListener(CallAndUserForSignals callAndUserForSignals,
                                CallService callService,
                                @Lazy SocketIOServer socketIOServer) {
        this.callAndUserForSignals = callAndUserForSignals;
        this.callService = callService;
        this.socketIOServer = socketIOServer;
    }


    @Override
    public void onData(SocketIOClient client, SignalAnswerRequest data, AckRequest ackSender) throws Exception {
        Long clientId = client.get("userId");
        if (clientId == null){
            client.disconnect();
            return;
        }
        Long callId =  data.callId();
        if (!callAndUserForSignals.isValid(data, client)) {
            return;
        }
        String sdp = data.sdpMessage();
        try {
            CallAndUser callAndUser = callAndUserForSignals.findCallAndUser(callId, clientId);
            callService.validateCallAccess(callAndUser.call(), callAndUser.user());
            socketIOServer.getRoomOperations("call-"+ String.valueOf(callId))
                    .sendEvent("signal:answer", client, sdp);
        } catch(ForbiddenException | ResourceNotFoundException e) {
            client.sendEvent("signal:error", e.getMessage());
        }
    }
}