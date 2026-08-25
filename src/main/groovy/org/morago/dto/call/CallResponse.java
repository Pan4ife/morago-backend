package org.morago.dto.call;


import org.morago.model.CallStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record CallResponse (

    Long id,

    String clientEmail,

    String translatorEmail,

    CallStatus status,

    LocalDateTime startTime,

    LocalDateTime endTime,

    Long durationSeconds,

    BigDecimal cost

) {

}
