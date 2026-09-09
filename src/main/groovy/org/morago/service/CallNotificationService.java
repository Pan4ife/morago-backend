package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.event.SocketNotificationEvent;
import org.morago.model.Call;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CallNotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public void notifyIncomingCall(Call call) {
        Long translatorId = call.getTranslator().getUser().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "call:incoming", call.getId()));
    }

    public void notifyCallStarted(Call call) {
        Long translatorId = call.getTranslator().getUser().getId();
        Long clientId = call.getClient().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(List.of(String.valueOf(translatorId), String.valueOf(clientId)),
                        "call:started", call.getId()));
    }

    public void notifyCallFinished(Call call) {
        Long translatorId = call.getTranslator().getUser().getId();
        Long clientId = call.getClient().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(List.of(String.valueOf(translatorId), String.valueOf(clientId)),
                        "call:finished", call.getId()));
    }

    public void notifyCallCancelled(Call call) {
        Long translatorId = call.getTranslator().getUser().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "call:cancelled", call.getId()));
    }
}