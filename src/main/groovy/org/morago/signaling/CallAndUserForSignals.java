package org.morago.signaling;

import com.corundumstudio.socketio.SocketIOClient;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.Call;
import org.morago.model.User;
import org.morago.repository.CallRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CallAndUserForSignals {
    private final CallRepository callRepository;
    private final UserRepository userRepository;
    private final Validator validator;

    public CallAndUser findCallAndUser(Long callId, Long userId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new CallAndUser(call, user);
    }

    public boolean isValid(Object data, SocketIOClient client) {
        Set<ConstraintViolation<Object>> violations = validator.validate(data);
        if (!violations.isEmpty()) {
            client.sendEvent("signal:error", violations.iterator().next().getMessage());
            return false;
        }
        return true;
    }
}
