package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.model.Call;
import org.morago.model.CallStatus;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.repository.CallRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;

    private final UserRepository userRepository;

    private final TranslatorProfileRepository translatorProfileRepository;


    public CallResponse create(
            String email,
            CallRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        TranslatorProfile translator = translatorProfileRepository.findById(request.getTranslatorId())
                .orElseThrow(() ->
                        new RuntimeException("Translator not found"));

        Call call = new Call();

        LocalDateTime now = LocalDateTime.now();

        call.setClient(user);

        call.setTranslator(translator);

        call.setStatus(CallStatus.CREATED);

        call.setStartTime(now);

        call.setCost(BigDecimal.ZERO);

        call.setCreatedAt(now);

        Call savedCall = callRepository.save(call);

        return new CallResponse(savedCall.getId(),
                savedCall.getClient().getEmail(),
                savedCall.getTranslator().getUser().getEmail(),
                savedCall.getStatus(),
                savedCall.getCost()
        );

    }

    public List<CallResponse> getAll() {

        return callRepository.findAll()
                .stream()
                .map(call ->
                        new CallResponse(
                                call.getId(),
                                call.getClient().getEmail(),
                                call.getTranslator().getUser().getEmail(),
                                call.getStatus(),
                                call.getCost()
                        )
                )
                .toList();
    }

    public void delete(Long id) {

        callRepository.deleteById(id);

    }

}
