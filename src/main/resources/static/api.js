// api.js — все запросы к серверу morago: одна функция request() с токенами, авто-обновлением и разбором ошибок.

const ACCESS_KEY = "morago.accessToken";
const REFRESH_KEY = "morago.refreshToken";

/** Ошибка API: статус, текст сообщения и ошибки полей из ErrorResponse. */
class ApiError extends Error {
    constructor(status, message, validationErrors) {
        super(message);
        this.status = status;
        this.validationErrors = validationErrors || {};
    }
}

const tokens = {
    access: () => localStorage.getItem(ACCESS_KEY),
    refresh: () => localStorage.getItem(REFRESH_KEY),
    save(pair) {
        localStorage.setItem(ACCESS_KEY, pair.accessToken);
        localStorage.setItem(REFRESH_KEY, pair.refreshToken);
    },
    clear() {
        localStorage.removeItem(ACCESS_KEY);
        localStorage.removeItem(REFRESH_KEY);
    },
    has: () => Boolean(localStorage.getItem(ACCESS_KEY))
};

// Вызывается, когда сессию восстановить нельзя (app.js показывает экран входа).
let onSessionExpired = () => {};

// Один общий запрос обновления на все одновременные 401.
let refreshInFlight = null;

async function readError(response) {
    let body = null;
    try {
        body = await response.json();
    } catch (e) {
        // тело не JSON — сообщение по статусу ниже
    }
    const message = (body && body.message) || ("Ошибка сервера (" + response.status + ")");
    return new ApiError(response.status, message, body && body.validationErrors);
}

async function readBody(response) {
    const text = await response.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch (e) {
        return text;
    }
}

function send(method, path, body) {
    const headers = {};
    const access = tokens.access();
    if (access) headers["Authorization"] = "Bearer " + access;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    return fetch(path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
    });
}

async function refreshTokens() {
    const refreshToken = tokens.refresh();
    if (!refreshToken) return false;
    const response = await fetch("/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken })
    });
    if (!response.ok) return false;
    tokens.save(await response.json());
    return true;
}

function refreshOnce() {
    if (!refreshInFlight) {
        refreshInFlight = refreshTokens()
            .catch(() => false)
            .finally(() => { refreshInFlight = null; });
    }
    return refreshInFlight;
}

/**
 * Единственная точка запросов к API. Ставит Bearer-токен; на 401 один раз обновляет пару токенов
 * и повторяет запрос. Не удалось обновить — стирает токены и сообщает app.js (экран входа).
 */
async function request(method, path, body) {
    let response = await send(method, path, body);

    if (response.status === 401 && tokens.access()) {
        const refreshed = await refreshOnce();
        if (!refreshed) {
            tokens.clear();
            onSessionExpired();
            throw await readError(response);
        }
        response = await send(method, path, body); // повтор ровно один раз
    }

    if (!response.ok) throw await readError(response);
    return readBody(response);
}

const api = {
    ApiError,
    tokens,
    setOnSessionExpired(handler) { onSessionExpired = handler; },

    register: (email, password) => request("POST", "/auth/register", { email, password }),

    async login(email, password) {
        tokens.clear(); // вход без старых токенов: 401 «неверный пароль» не должен запускать обновление
        const pair = await request("POST", "/auth/login", { email, password });
        tokens.save(pair);
    },

    me: () => request("GET", "/auth/me"),

    translators: (page = 0, size = 20) =>
        request("GET", "/translators?page=" + encodeURIComponent(page) + "&size=" + encodeURIComponent(size)),

    myTranslatorProfile: () => request("GET", "/translator-profile/me"),

    setOnline: online => request("PATCH", "/translator-profile/online", { online }),

    createCall: translatorId => request("POST", "/calls", { translatorId }),

    cancelCall: callId => request("PATCH", "/calls/" + encodeURIComponent(callId) + "/cancel"),

    async logout() {
        try {
            await request("POST", "/auth/logout");
        } finally {
            tokens.clear();
        }
    }
};

window.api = api;
