package org.morago.dto.translatorprofile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class    TranslatorProfileResponse {

    private Long id;

    private String email;

    private String bio;

    private Double rating;

    private boolean online;

}
