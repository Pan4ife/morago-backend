// call-screen.js — экран исходящего звонка «Ждём переводчика…» с кнопкой «Отменить» (запросы — через api.js).

const callScreen = (() => {
    const el = {
        translator: document.getElementById("call-translator"),
        error: document.getElementById("call-error"),
        cancel: document.getElementById("call-cancel")
    };

    const CANCEL_ERRORS = {
        409: "Звонок уже нельзя отменить.",
        404: "Звонок не найден."
    };

    let call = null;
    let onClosed = () => {};

    function setError(text) {
        el.error.textContent = text || "";
        el.error.hidden = !text;
    }

    async function onCancel() {
        if (!call) return;
        el.cancel.disabled = true;
        setError("");
        try {
            await api.cancelCall(call.id);
            call = null;
            onClosed();
        } catch (error) {
            setError(errorText(error, CANCEL_ERRORS));
        } finally {
            el.cancel.disabled = false;
        }
    }

    function init(handlers) {
        onClosed = handlers.onClosed;
        el.cancel.addEventListener("click", onCancel);
    }

    function open(createdCall) {
        call = createdCall;
        el.translator.textContent = createdCall.translatorEmail || "";
        setError("");
    }

    return { init, open };
})();

window.callScreen = callScreen;
