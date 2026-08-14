package org.morago.dto.review;

import jakarta.validation.constraints.*;

public record ReviewRequest (

    @NotNull
    Long callId,

    @Min(1)
    @Max(5)
    Integer rating,

    @NotBlank
    String comment
) {

}
