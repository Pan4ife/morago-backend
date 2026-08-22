package org.morago.dto.review;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotNull
    private Long callId;

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;
}
