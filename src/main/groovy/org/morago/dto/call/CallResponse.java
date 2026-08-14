package org.morago.dto.call;


import org.morago.model.CallStatus;

import java.math.BigDecimal;


public record CallResponse (

    Long id,

    String clientEmail,

    String translatorEmail,

    CallStatus status,

    BigDecimal cost

) {

}
