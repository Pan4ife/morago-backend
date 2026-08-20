package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.exception.ConflictException;
import org.morago.exception.ForbiddenException;
import org.morago.exception.InvalidCallStatusTransitionException;
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
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;

    private final UserRepository userRepository;

    private final TranslatorProfileRepository translatorProfileRepository;

    private static final Map<CallStatus, Set<CallStatus>> ALLOWED_TRANSITIONS =
            Map.of(
            CallStatus.CREATED, Set.of(CallStatus.IN_PROGRESS, CallStatus.CANCELLED),
            CallStatus.IN_PROGRESS, Set.of(CallStatus.FINISHED),
            CallStatus.FINISHED, Set.of(),
            CallStatus.CANCELLED, Set.of()
    );
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

    private void validateCallStatusTransition(CallStatus from, CallStatus to) {
        Set<CallStatus> allowedStatues = ALLOWED_TRANSITIONS.get(from);
        boolean isAllowed = allowedStatues.contains(to);
        if (!isAllowed) {
            throw new InvalidCallStatusTransitionException("Invalid call status");
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

    private void validateCallAccess(Call call, User user) {
        if (!isAdmin(user) && !call.getClient().getId().equals(user.getId()) &&
                !call.getTranslator().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    public CallResponse getById(Long id, String email) {
        Call currentCall = callRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Call not found"));
        User currentUser = getCurrentUser(email);
        validateCallAccess(currentCall, currentUser);
        return mapToResponse(currentCall);
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
        if(!translator.isOnline()){
            throw new ConflictException("Translator is not available now");
        }
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
                    .map(this::mapToResponse)
                    .toList();
        }

        return callRepository.findByClient(currentUser)
                .stream()
                .map(this::mapToResponse)
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
        validateCallStatusTransition(call.getStatus(), CallStatus.FINISHED);
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
        validateCallStatusTransition(call.getStatus(), CallStatus.CANCELLED);
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
        validateCallStatusTransition(call.getStatus(), CallStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();
        call.setStatus(CallStatus.IN_PROGRESS);
        call.setStartTime(now);
        call.setUpdatedAt(now);
        Call savedCall = callRepository.save(call);
        log.info("Call {} was started by user {}", id, email);
        return mapToResponse(savedCall);
    }
}
