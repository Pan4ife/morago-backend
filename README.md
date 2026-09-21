Morago Backend
Backend-приложение для платформы бронирования устных переводчиков. Пользователи бронируют переводчика на звонок (аудио/видео через WebRTC), система обрабатывает оплату, начисления переводчику и вывод средств.

Стек технологий
Java 25, Spring Boot 4.1.0, mixed Java/Groovy проект (gmavenplus-plugin)
Spring Security 6 — аутентификация и авторизация
Spring Data JPA / Hibernate — работа с базой данных
MySQL — основная СУБД
JWT (jjwt 0.12.7) — access/refresh токены
netty-socketio 2.0.13 — WebSocket-инфраструктура для сигналинга звонков (WebRTC)
springdoc-openapi 2.8.9 — автогенерация Swagger-документации
Lombok — сокращение boilerplate-кода
JUnit 5 + Testcontainers — тестирование с реальным MySQL в контейнере
Основной функционал
Аутентификация и авторизация — регистрация, логин, JWT + refresh-токены, ролевая модель (RBAC)
Профили переводчиков — создание и управление профилем переводчика, языки и темы специализации
Звонки — бронирование звонка с переводчиком, сигналинг через WebSocket/Socket.io, статусы звонка (CallStatus)
Транзакции и вывод средств — оплата звонков, история транзакций, заявки на вывод средств (WithdrawalRequest) со статусами
Отзывы — оценка и отзывы о переводчиках после звонка
Админ-панель — управление пользователями и платформой через отдельные административные эндпоинты
Аудит-лог — журналирование ключевых действий в системе
Структура проекта
``` src/main/groovy/org/morago/ ├── config/ # конфигурация Spring (Security, WebSocket, OpenAPI и т.д.) ├── controller/ # REST-контроллеры (Auth, Call, Admin, Transaction, Review, ...) ├── dto/ # DTO по доменам (auth, call, transaction, withdrawal, ...) ├── event/ # доменные события ├── exception/ # кастомные исключения и глобальный обработчик ошибок ├── model/ # JPA-сущности (User, Call, Transaction, TranslatorProfile, ...) ├── repository/ # Spring Data репозитории ├── scheduler/ # плановые задачи ├── security/ # JWT, фильтры аутентификации, RBAC ├── service/ # бизнес-логика ├── signaling/ # WebRTC-сигналинг поверх Socket.io └── util/ # вспомогательные утилиты ```

Запуск локально
Требования
JDK 25
MySQL (локально или через Docker)
Maven (через ./mvnw / mvnw.cmd, отдельная установка не нужна)
Переменные окружения
Переменная	Назначение
DB_PASSWORD	пароль пользователя MySQL
JWT_SECRET	секрет для подписи JWT (минимум 32 символа)
SPRING_PROFILES_ACTIVE	активный профиль (по умолчанию dev)
SOCKETIO_HOST / SOCKETIO_PORT	адрес и порт для Socket.io-сервера (по умолчанию localhost:9092)
Сборка и запуск
```bash ./mvnw clean package -DskipTests java -jar target/morago-0.0.1-SNAPSHOT.jar ```

Приложение поднимется на http://localhost:8080 (порт зависит от конфигурации).

API-документация
После запуска Swagger UI доступен по адресу:

``` http://localhost:8080/swagger-ui/index.html ```

Тестирование
```bash ./mvnw test ```

Интеграционные тесты используют Testcontainers — для их прохождения нужен запущенный Docker.

Демо-деплой
Рабочая версия backend'а развёрнута на Railway: ссылка на демо
