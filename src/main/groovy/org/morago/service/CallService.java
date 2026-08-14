package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.exception.AccessDeniedException;
import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.CallRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;

    private final UserRepository userRepository;

    private final TranslatorProfileRepository translatorProfileRepository;
    private final TransactionService transactionService;


    private boolean isAdmin(User user) {
        return user.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    private void validateCallStatus(Call call) {
        if (call.getStatus() == CallStatus.FINISHED) {
            throw new RuntimeException("Call already finished");
        }

        if (call.getStatus() == CallStatus.CANCELLED) {
            throw new RuntimeException("Call already cancelled");
        }
    }

    private void validateTranslatorAccess(Call call, User user) {

        if (!isAdmin(user) &&
                !call.getTranslator()
                        .getUser()
                        .getId()
                        .equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void validateClientAccess(Call call, User user) {
        if (!isAdmin(user) &&
                !call.getClient()
                        .getId()
                        .equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
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

        TranslatorProfile translator = translatorProfileRepository.findById(request.translatorId())
                .orElseThrow(() ->
                        new RuntimeException("Translator not found"));

        Call call = new Call();

        LocalDateTime now = LocalDateTime.now();

        call.setClient(user);

        call.setTranslator(translator);

        call.setStatus(CallStatus.CREATED);

        call.setStartTime(null);

        call.setCost(BigDecimal.ZERO);

        call.setCreatedAt(now);

        Call savedCall = callRepository.save(call);

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
            throw new AccessDeniedException("Access denied");
        }

        Call call = callRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Call not found"));

        callRepository.delete(call);

    }

    /**
     * Завершает звонок и рассчитывает его стоимость.
     * <p>
     * Правило округления: длительность звонка считается пропорционально
     * по секундам (например, 7 минут 30 секунд = 7.5 минуты), без округления
     * вверх/вниз до целой минуты. Итоговая стоимость округляется до 2 знаков
     * после запятой (копейки) по правилу HALF_UP (0.5 округляется в большую сторону).
     */
    @Transactional
    public CallResponse finish(Long id, String email) {

        Call call = callRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);


        validateTranslatorAccess(call, currentUser);

        if (call.getStatus() != CallStatus.IN_PROGRESS) {
            throw new ConflictException("Call can only be finished from IN_PROGRESS status");
        }

        LocalDateTime now = LocalDateTime.now();

        long seconds = Duration.between(call.getStartTime(), now).toSeconds();

        if (seconds < 0) {
            throw new ConflictException("Invalid call duration");
        }

        BigDecimal durationInMinutes = BigDecimal.valueOf(seconds)
                        .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        BigDecimal costPerMinutes = call.getTranslator().getHourlyRate()
                        .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        BigDecimal cost = durationInMinutes
                .multiply(costPerMinutes)
                        .setScale(2, RoundingMode.HALF_UP);

        call.setStatus(CallStatus.FINISHED);
        call.setEndTime(now);
        call.setUpdatedAt(now);
        call.setCost(cost);

        User client = call.getClient();
        User translator = call.getTranslator().getUser();

        transactionService.payForCall(client, translator, cost, call);

        Call savedCall = callRepository.save(call);

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

        return mapToResponse(savedCall);

    }

}
