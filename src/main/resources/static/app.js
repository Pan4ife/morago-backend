// app.js — логика экранов: вход, регистрация, шапка пользователя, выход и переходы каталог ⇄ звонок.

const el = {
    loading: document.getElementById("loading"),
    authScreen: document.getElementById("auth-screen"),
    homeScreen: document.getElementById("home-screen"),
    callScreen: document.getElementById("call-screen"),
    tabs: document.querySelectorAll(".tab"),
    form: document.getElementById("auth-form"),
    email: document.getElementById("email"),
    password: document.getElementById("password"),
    submit: document.getElementById("auth-submit"),
    formError: document.getElementById("auth-error"),
    fieldErrors: document.querySelectorAll(".field-error"),
    userEmail: document.getElementById("user-email"),
    userRoles: document.getElementById("user-roles"),
    userBalance: document.getElementById("user-balance"),
    logout: document.getElementById("logout-button")
};

let mode = "login"; // "login" | "register"

// Понятные тексты для известных ответов; остальное — message с сервера.
const FRIENDLY = {
    login: {
        401: "Неверный пароль или аккаунт заблокирован.",
        404: "Пользователь с таким email не найден."
    },
    register: {
        409: "Этот email уже занят."
    }
};

function show(screen) {
    el.loading.hidden = true;
    el.authScreen.hidden = screen !== "auth";
    el.homeScreen.hidden = screen !== "home";
    el.callScreen.hidden = screen !== "call";
}

function clearErrors() {
    el.formError.hidden = true;
    el.formError.textContent = "";
    el.fieldErrors.forEach(node => { node.textContent = ""; });
}

function showError(error, action) {
    const validation = error.validationErrors || {};
    const unplaced = [];

    Object.entries(validation).forEach(([field, text]) => {
        const node = document.querySelector('.field-error[data-field="' + CSS.escape(field) + '"]');
        if (node) node.textContent = text;
        else unplaced.push(text);
    });

    const friendly = FRIENDLY[action] && FRIENDLY[action][error.status];
    let text = errorText(error, FRIENDLY[action]);
    if (Object.keys(validation).length > 0 && !friendly) {
        text = unplaced.length > 0 ? unplaced.join(" ") : "Проверьте поля формы.";
    }

    el.formError.textContent = text;
    el.formError.hidden = false;
}

function setMode(newMode) {
    mode = newMode;
    el.tabs.forEach(tab => tab.classList.toggle("active", tab.dataset.mode === mode));
    el.submit.textContent = mode === "login" ? "Войти" : "Зарегистрироваться";
    el.password.autocomplete = mode === "login" ? "current-password" : "new-password";
    clearErrors();
}

function renderUser(me) {
    el.userEmail.textContent = me.email;
    el.userRoles.textContent = (me.roles || []).slice().sort().join(", ");
    el.userBalance.textContent = me.balance == null ? "—" : String(me.balance);
}

function showLogin() {
    el.password.value = "";
    setMode("login");
    show("auth");
}

async function loadHome() {
    const me = await api.me();
    renderUser(me);
    show("home");
    // свои ошибки каталог и переключатель показывают сами — вход от них не ломается
    onlineToggle.open(me);
    catalog.open(me);
}

function onCallStarted(call) {
    callScreen.open(call);
    show("call");
}

function onCallClosed() {
    show("home");
    catalog.reload();
}

async function onSubmit(event) {
    event.preventDefault();
    clearErrors();
    const action = mode;
    const email = el.email.value.trim();
    const password = el.password.value;

    el.submit.disabled = true;
    try {
        if (action === "register") {
            await api.register(email, password);
        }
        await api.login(email, password); // после регистрации — сразу вход теми же данными
        el.password.value = "";
        await loadHome();
    } catch (error) {
        showError(error, action);
    } finally {
        el.submit.disabled = false;
    }
}

async function onLogout() {
    el.logout.disabled = true;
    try {
        await api.logout();
    } catch (error) {
        // ошибка logout не мешает выйти: токены уже стёрты в api.logout()
    } finally {
        el.logout.disabled = false;
        showLogin();
    }
}

async function start() {
    api.setOnSessionExpired(showLogin);
    el.tabs.forEach(tab => tab.addEventListener("click", () => setMode(tab.dataset.mode)));
    el.form.addEventListener("submit", onSubmit);
    el.logout.addEventListener("click", onLogout);
    catalog.init({ onCallStarted });
    onlineToggle.init({ onChanged: () => catalog.reload() });
    callScreen.init({ onClosed: onCallClosed });

    if (!api.tokens.has()) {
        showLogin();
        return;
    }
    try {
        await loadHome(); // после перезагрузки: токены из localStorage, шапка из GET /auth/me
    } catch (error) {
        if (error.status === 401 || error.status === 404) api.tokens.clear();
        showLogin();
        showError(error, "me");
    }
}

start();
