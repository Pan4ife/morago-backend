package org.morago.dto.call;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.morago.model.CallStatus;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CallResponse {

    private Long id;

    private String clientEmail;

    private String translatorEmail;

    private CallStatus status;

    private BigDecimal cost;

}
