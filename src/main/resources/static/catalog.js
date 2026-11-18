// catalog.js — каталог проверенных переводчиков: список, «Обновить» и кнопка «Позвонить» (запросы — через api.js).

const catalog = (() => {
    const el = {
        list: document.getElementById("catalog-list"),
        empty: document.getElementById("catalog-empty"),
        error: document.getElementById("catalog-error"),
        refresh: document.getElementById("catalog-refresh")
    };

    const CALL_ERRORS = {
        409: "Переводчик сейчас не на связи или звонок невозможен. Обновите список.",
        404: "Переводчик не найден. Обновите список.",
        403: "Звонить могут только клиенты."
    };

    let me = null;
    let onCallStarted = () => {};
    let loadSeq = 0;      // ответ старой загрузки не перетирает новую
    let calling = false;  // один звонок за раз: двойное нажатие не создаёт второй

    function setError(text) {
        el.error.textContent = text || "";
        el.error.hidden = !text;
    }

    // Ставка за час и за минуту (ставка / 60, до двух знаков): «600 в час (10 в минуту)».
    function formatRate(hourlyRate) {
        if (hourlyRate == null) return "Ставка не указана";
        const hour = Number(hourlyRate);
        const minute = Number((hour / 60).toFixed(2));
        return hour + " в час (" + minute + " в минуту)";
    }

    function canCall(translator) {
        const roles = me && me.roles ? me.roles : [];
        return roles.includes("USER")
            && translator.online
            && translator.id !== me.translatorProfileId;
    }

    function textNode(tag, className, text) {
        const node = document.createElement(tag);
        node.className = className;
        node.textContent = text;
        return node;
    }

    async function call(translator, button) {
        if (calling) return;
        calling = true;
        button.disabled = true;
        setError("");
        try {
            const created = await api.createCall(translator.id);
            onCallStarted(created);
        } catch (error) {
            setError(errorText(error, CALL_ERRORS));
        } finally {
            calling = false;
            button.disabled = false;
        }
    }

    function renderItem(translator) {
        const item = document.createElement("li");
        item.className = "translator";

        const info = document.createElement("div");
        info.className = "translator-info";
        info.append(
            textNode("span", "translator-email", translator.email || ""),
            textNode("span", "translator-bio", translator.bio || ""),
            textNode("span", "translator-rate", formatRate(translator.hourlyRate)),
            textNode("span", translator.online ? "status online" : "status offline",
                translator.online ? "на связи" : "не на связи")
        );
        item.append(info);

        if (canCall(translator)) {
            const button = textNode("button", "primary call-button", "Позвонить");
            button.type = "button";
            button.addEventListener("click", () => call(translator, button));
            item.append(button);
        }
        return item;
    }

    function render(translators) {
        el.list.replaceChildren(...translators.map(renderItem));
        el.empty.hidden = translators.length > 0;
    }

    async function reload() {
        const seq = ++loadSeq;
        el.refresh.disabled = true;
        setError("");
        try {
            const page = await api.translators(0, 20);
            if (seq !== loadSeq) return;
            render((page && page.content) || []);
        } catch (error) {
            if (seq !== loadSeq) return;
            render([]);
            el.empty.hidden = true;
            setError("Не удалось загрузить список переводчиков. " + errorText(error));
        } finally {
            if (seq === loadSeq) el.refresh.disabled = false;
        }
    }

    function init(handlers) {
        onCallStarted = handlers.onCallStarted;
        el.refresh.addEventListener("click", reload);
    }

    function open(currentUser) {
        me = currentUser;
        return reload();
    }

    return { init, open, reload };
})();

window.catalog = catalog;
