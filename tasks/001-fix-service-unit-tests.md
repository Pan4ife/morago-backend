---
id: 001
title: Починить unit-тесты TransactionService и WithdrawalRequestService
role: qa
tests: TransactionServiceTest, WithdrawalRequestServiceTest
max_tries: 3
---

## Цель
6 unit-тестов в `TransactionServiceTest` и `WithdrawalRequestServiceTest` падают с NullPointerException.
После задания все 10 тестов этих двух классов зелёные.

## Контекст
- `src/test/groovy/org/morago/service/TransactionServiceTest.java`, `WithdrawalRequestServiceTest.java`.
- Сервисы: `src/main/groovy/org/morago/service/TransactionService.java`, `WithdrawalRequestService.java`.
- Тесты падают на вызовах `paymentNotificationService.*` — у сервиса появилась зависимость, а тесты о ней не знают.

## Критерии приёмки (проверяют автомат и судья)
1. Все тесты `TransactionServiceTest` и `WithdrawalRequestServiceTest` зелёные (5 + 5).
2. Ни один существующий тест не удалён, не отключён, проверки (assert/verify) не ослаблены.
3. Там, где тест проверяет действие с деньгами (пополнение, оплата, создание/одобрение/отклонение заявки),
   проверено и то, что сервис отправил соответствующее уведомление.
4. Код в `src/main/` не изменён.

## Чек-лист для глаз (проверяет человек)
- [ ] Дифф маленький и понятный: добавлена заглушка зависимости и проверки уведомлений.

## Вне рамок
- Интеграционные тесты с Testcontainers (`SecurityIntegrationTests`, `CallServiceConcurrencyTest`) — им нужен Docker.
- Любые правки сервисов.
