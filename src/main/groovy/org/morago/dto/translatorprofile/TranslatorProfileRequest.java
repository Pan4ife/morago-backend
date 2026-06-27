package org.morago.dto.translatorprofile;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TranslatorProfileRequest {

    private String bio;

    private Set<Long> languageIds;

    private Set<Long> topicIds;
}
