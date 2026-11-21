package org.morago.dto.translatorprofile;

import jakarta.validation.constraints.NotNull;

/** Запрос переводчика на включение или выключение статуса «На связи». */
public record OnlineStatusRequest(
        @NotNull
        Boolean online
){
}
