# morago — правила проекта

Платформа звонков с переводчиком: Spring Boot бэкенд (Java/Groovy, MySQL, Socket.IO-сигналинг для WebRTC)
и простой веб-клиент. Здесь же стоит упрощённый конвейер агентов (копия из project-keys-v2) — эксперимент,
итоги в `experiment.md`.

## Стек
- Java 25, Spring Boot 4.1, Maven (`./mvnw`), код в `src/main/groovy/org/morago/`.
- Тесты JUnit 5 + Mockito; интеграционные — Testcontainers (нужен Docker).
- Веб-клиент — HTML/CSS/JS без сборщика в `src/main/resources/static/`, раздаётся самим Spring Boot.
- Сигналинг: netty-socketio, события описаны в `docs/socketio-events.md`.

## Конвейер
```
задание tasks/NNN-*.md → run_task.py: проверка main (запоминает красные тесты) → ветка task/NNN-*
   → исполнитель (claude -p, роль) ⇄ verify.py (scope → сборка+тесты → судья), до max_tries
   → зелёный → коммит в ветку → reports/NNN/report.md, metrics.json → PR открывает агент, сливает человек
```
- Запуск: `uv run --no-project conveyor/run_task.py tasks/NNN-имя.md`.
- Роли и разрешённые папки — `conveyor/roles/*.md`. Тронул чужое — `scope` красный.
- Тесты: сборка проходит, тестов не меньше, чем на main, нет новых красных, тесты из `tests:` задания зелёные.
- Исполнитель не коммитит, не трогает `conveyor/`, `tasks/`, `CLAUDE.md`, `pom.xml`.

## Правила кода
- Секреты (`JWT_SECRET`, `DB_PASSWORD`) — только из переменных окружения, в файлы не писать.
- Не ослаблять и не удалять тесты. Новое поведение — с тестом.
- Не хватает решения — стоп и вопрос в `reports/NNN/questions.md`, не угадывать.
