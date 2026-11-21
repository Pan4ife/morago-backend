// online-toggle.js — переключатель «На связи» для переводчика с анкетой (запросы — через api.js).

const onlineToggle = (() => {
    const el = {
        box: document.getElementById("online-box"),
        input: document.getElementById("online-toggle"),
        error: document.getElementById("online-error")
    };

    const ONLINE_ERRORS = {
        409: "Анкета ещё не подтверждена."
    };

    let onChanged = () => {};

    function setError(text) {
        el.error.textContent = text || "";
    }

    function isTranslator(me) {
        return (me.roles || []).includes("TRANSLATOR") && me.translatorProfileId != null;
    }

    async function onToggle() {
        const wanted = el.input.checked;
        el.input.disabled = true;
        setError("");
        try {
            const profile = await api.setOnline(wanted);
            el.input.checked = Boolean(profile && profile.online);
            onChanged();
        } catch (error) {
            el.input.checked = !wanted; // сервер не принял — возвращаем прежнее состояние
            setError(errorText(error, ONLINE_ERRORS));
        } finally {
            el.input.disabled = false;
        }
    }

    function init(handlers) {
        onChanged = handlers.onChanged;
        el.input.addEventListener("change", onToggle);
    }

    async function open(me) {
        setError("");
        el.box.hidden = !isTranslator(me);
        if (el.box.hidden) return;

        el.input.disabled = true;
        try {
            const profile = await api.myTranslatorProfile();
            el.input.checked = Boolean(profile && profile.online);
            el.input.disabled = false;
        } catch (error) {
            setError("Не удалось узнать статус «На связи». " + errorText(error));
        }
    }

    return { init, open };
})();

window.onlineToggle = onlineToggle;
