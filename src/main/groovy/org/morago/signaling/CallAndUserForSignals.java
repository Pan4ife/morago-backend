package org.morago.signaling;

import lombok.RequiredArgsConstructor;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.Call;
import org.morago.model.User;
import org.morago.repository.CallRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CallAndUserForSignals {
    private final CallRepository callRepository;
    private final UserRepository userRepository;

    public CallAndUser findCallAndUser(Long callId, Long userId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new CallAndUser(call, user);
    }
}
