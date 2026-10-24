---
role: backend
allowed:
  - src/main/groovy/**
  - src/main/resources/*.properties
  - src/test/**
---
# Роль: backend (сервер)

**Делает:** REST-контроллеры, сервисы, DTO, безопасность, сигналинг в `src/main/groovy/`, и тесты к своему коду.

**Не делает:** веб-клиент (`src/main/resources/static/`), зависимости в `pom.xml` (это решает человек).

**Готово, когда:** сборка и тесты зелёные, новых красных тестов нет, у нового поведения есть тест.
