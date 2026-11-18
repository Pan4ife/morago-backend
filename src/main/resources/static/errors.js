// errors.js — понятный пользователю текст ошибки запроса: свой текст по статусу, иначе сообщение сервера.

/**
 * friendly — { статус: текст }. Нет связи (fetch упал) — «Нет связи с сервером.».
 */
function errorText(error, friendly) {
    if (error instanceof TypeError) return "Нет связи с сервером.";
    const own = friendly && friendly[error.status];
    return own || error.message || "Что-то пошло не так. Попробуйте ещё раз.";
}

window.errorText = errorText;
