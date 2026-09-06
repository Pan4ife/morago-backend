package org.morago.scheduler;

import lombok.RequiredArgsConstructor;
import org.morago.model.Call;
import org.morago.repository.CallRepository;
import org.morago.service.CallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CallTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(CallTimeoutScheduler.class);

    private static final long TIMEOUT_MINUTES = 60;

    private final CallRepository callRepository;
    private final CallService callService;

    @Scheduled(fixedRate = 5 * 60 * 1000) // каждые 5 минут
    public void finishStaleCalls() {

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Call> staleCalls = callRepository.findStaleInProgressCalls(threshold);

        for (Call call : staleCalls) {
            try {
                callService.finishByTimeout(call.getId());
                log.info("Call {} auto-finished due to timeout", call.getId());
            } catch (Exception e) {
                log.error("Failed to auto-finish call {}: {}", call.getId(), e.getMessage());
            }
        }
    }
}
