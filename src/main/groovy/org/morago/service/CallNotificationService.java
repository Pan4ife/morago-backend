package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.model.Call;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CallNotificationService {
    private void notifyIncomingCall(Call call){
        Long translatorId = call.getTranslator().getUser().getId();
    }
}
