package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.exception.ForbiddenException;
import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.CallRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;

    private final UserRepository userRepository;

    private final TranslatorProfileRepository translatorProfileRepository;

    private static final Logger log = LoggerFactory.getLogger(CallService.class);


    private boolean isAdmin(User user) {
        return user.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private void validateCallStatus(Call call) {
        if (call.getStatus() == CallStatus.FINISHED) {
            throw new ConflictException("Call already finished");
        }

        if (call.getStatus() == CallStatus.CANCELLED) {
            throw new ConflictException("Call already cancelled");
        }
    }

    private void validateTranslatorAccess(Call call, User user) {

        if (!isAdmin(user) &&
                !call.getTranslator()
                        .getUser()
                        .getId()
                        .equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void validateClientAccess(Call call, User user) {
        if (!isAdmin(user) &&
                !call.getClient()
                        .getId()
                        .equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private CallResponse mapToResponse(Call call) {

        return new CallResponse(
                call.getId(),
                call.getClient().getEmail(),
                call.getTranslator().getUser().getEmail(),
                call.getStatus(),
                call.getCost()
        );
    }


    public CallResponse create(
            String email,
            CallRequest request
    ) {

        User user = getCurrentUser(email);

        TranslatorProfile translator = translatorProfileRepository.findById(request.getTranslatorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Translator not found"));

        Call call = new Call();

        LocalDateTime now = LocalDateTime.now();

        call.setClient(user);

        call.setTranslator(translator);

        call.setStatus(CallStatus.CREATED);

        call.setStartTime(null);

        call.setCost(BigDecimal.ZERO);

        call.setCreatedAt(now);

        Call savedCall = callRepository.save(call);

        log.info("Call {} created by user  {}", savedCall.getId(), email);

        return mapToResponse(savedCall);

    }

    public List<CallResponse> getAll(String email) {

        User currentUser = getCurrentUser(email);

        if (isAdmin(currentUser)) {

            return callRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (currentUser.getTranslatorProfile() != null) {

            return callRepository.findByTranslator_User(currentUser)
                    .stream()
                    .map(call -> new CallResponse(
                            call.getId(),
                            call.getClient().getEmail(),
                            call.getTranslator().getUser().getEmail(),
                            call.getStatus(),
                            call.getCost()
                    ))
                    .toList();
        }

        return callRepository.findByClient(currentUser)
                .stream()
                .map(call -> new CallResponse(
                        call.getId(),
                        call.getClient().getEmail(),
                        call.getTranslator().getUser().getEmail(),
                        call.getStatus(),
                        call.getCost()
                ))
                .toList();

    }

    public void delete(Long id, String email) {

        User currentUser = getCurrentUser(email);

        boolean admin = isAdmin(currentUser);

        if (!admin) {
            throw new ForbiddenException("Access denied");
        }

        Call call = callRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Call not found"));

        callRepository.delete(call);

        log.info("Call {} was deleted by user {}", id, email);
    }

    public CallResponse finish(Long id, String email) {

        Call call = callRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);


        validateTranslatorAccess(call, currentUser);

        if (call.getStatus() != CallStatus.IN_PROGRESS) {
            throw new ConflictException("Call can only be finished from IN_PROGRESS status");
        }

            LocalDateTime now = LocalDateTime.now();

        call.setStatus(CallStatus.FINISHED);

        call.setEndTime(now);

        call.setUpdatedAt(now);

        Duration.between(call.getStartTime(), now);

        Call savedCall = callRepository.save(call);

        log.info("Call {} was finished by user {}", id, email);

        return mapToResponse(savedCall);

    }

    public CallResponse cancel(Long id, String email) {

        Call call = callRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);



        validateClientAccess(call, currentUser);


        validateCallStatus(call);


        LocalDateTime now = LocalDateTime.now();

        call.setStatus(CallStatus.CANCELLED);

        call.setEndTime(now);

        call.setUpdatedAt(now);

        Call savedCall = callRepository.save(call);

        log.info("Call {} was canceled by user {}", id, email);

        return mapToResponse(savedCall);

    }

    public CallResponse start(Long id, String email) {

        Call call = callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);



        validateTranslatorAccess(call, currentUser);

        if (call.getStatus() != CallStatus.CREATED) {
            throw new ConflictException("Call can only be started from CREATED status");
        }


        LocalDateTime now = LocalDateTime.now();

        call.setStatus(CallStatus.IN_PROGRESS);

        call.setStartTime(now);
        call.setUpdatedAt(now);

        Call savedCall = callRepository.save(call);

        log.info("Call {} was started by user {}", id, email);

        return mapToResponse(savedCall);

    }

}
