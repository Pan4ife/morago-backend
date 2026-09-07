package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.exception.*;
import org.morago.model.*;
import org.morago.repository.CallRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final UserRepository userRepository;
    private final TranslatorProfileRepository translatorProfileRepository;
    private final CallNotificationService callNotificationService;
    private final TransactionService transactionService;

    private static final Logger log = LoggerFactory.getLogger(CallService.class);

    private static final Map<CallStatus, Set<CallStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    CallStatus.CREATED, Set.of(CallStatus.IN_PROGRESS, CallStatus.CANCELLED),
                    CallStatus.IN_PROGRESS, Set.of(CallStatus.FINISHED),
                    CallStatus.FINISHED, Set.of(),
                    CallStatus.CANCELLED, Set.of()
            );

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

    public void validateCallAccess(Call call, User user) {
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
                call.getStartTime(),
                call.getEndTime(),
                call.getDurationSeconds(),
                call.getCost()
        );
    }

    @Transactional
    public CallResponse create(
            String email,
            CallRequest request
    ) {
        User user = getCurrentUser(email);

        TranslatorProfile translator = translatorProfileRepository.findById(request.translatorId())
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
        callNotificationService.notifyIncomingCall(savedCall);
        log.info("Call {} created by user  {}", savedCall.getId(), email);
        return mapToResponse(savedCall);
    }

    public Page<CallResponse> getAll(String email, Pageable pageable) {
        User currentUser = getCurrentUser(email);

        if (isAdmin(currentUser)) {

            return callRepository.findAll(pageable).map(this::mapToResponse);
        }

        if (currentUser.getTranslatorProfile() != null) {

            return callRepository.findByTranslator_User(currentUser, pageable)
                    .map(this::mapToResponse);
        }

        return callRepository.findByClient(currentUser, pageable)
                .map(this::mapToResponse);
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
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);
        validateTranslatorAccess(call, currentUser);
        validateCallStatusTransition(call.getStatus(), CallStatus.FINISHED);

        Call savedCall = finishInternal(call);
        callNotificationService.notifyCallFinished(savedCall);
        log.info("Call {} was finished by user {}", id, email);
        return mapToResponse(savedCall);
    }

    private Call finishInternal(Call call) {

        LocalDateTime now = LocalDateTime.now();

        long seconds = Duration.between(call.getStartTime(), now).toSeconds();

        if (seconds < 0) {
            throw new ConflictException("Invalid call duration");
        }

        BigDecimal durationInMinutes = BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        BigDecimal costPerMinute = call.getTranslator().getHourlyRate()
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        BigDecimal cost = durationInMinutes
                .multiply(costPerMinute)
                .setScale(2, RoundingMode.HALF_UP);

        call.setStatus(CallStatus.FINISHED);
        call.setEndTime(now);
        call.setUpdatedAt(now);
        call.setDurationSeconds(seconds);
        call.setCost(cost);

        User client = call.getClient();
        User translator = call.getTranslator().getUser();

        transactionService.payForCall(client, translator, cost, call);

        return callRepository.save(call);
    }

    @Transactional
    public void finishByTimeout(Long id) {
        Call call = callRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        validateCallStatusTransition(call.getStatus(), CallStatus.FINISHED);
        Call savedCall = finishInternal(call);
        callNotificationService.notifyCallFinished(savedCall);
    }

    @Transactional
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
        callNotificationService.notifyCallCancelled(savedCall);
        log.info("Call {} was canceled by user {}", id, email);
        return mapToResponse(savedCall);
    }

    @Transactional
    public CallResponse start(Long id, String email) {

        Call call = callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));

        User currentUser = getCurrentUser(email);
        validateTranslatorAccess(call, currentUser);
        validateCallStatusTransition(call.getStatus(), CallStatus.IN_PROGRESS);

        User client = call.getClient();
        if (client.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InsufficientBalanceException("Недостаточно средств у клиента для начала звонка");
        }

        LocalDateTime now = LocalDateTime.now();
        call.setStatus(CallStatus.IN_PROGRESS);
        call.setStartTime(now);
        call.setUpdatedAt(now);
        Call savedCall = callRepository.save(call);
        callNotificationService.notifyCallStarted(savedCall);
        log.info("Call {} was started by user {}", id, email);
        return mapToResponse(savedCall);
    }
}
