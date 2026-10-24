# Отчёт: 002 — Сигналинг принимает JWT из параметра token в адресе подключения

Ветка `task/002-socket-token-query`, попытка 1. Итог: **ЗЕЛЁНЫЙ**

| Проверка | Итог | Кратко |
|---|---|---|
| scope | PASS | изменено файлов: 2, все в папках роли |
| build_tests | PASS | тестов 18 (было 12), красных 8 — все были красными на main |
| judge | PASS | судья sonnet: принято |

## scope
```
src/main/groovy/org/morago/config/JwtAuthorizationListener.java
src/test/groovy/org/morago/config/JwtAuthorizationListenerTest.java
```

## build_tests
```
Красные сейчас: CallServiceConcurrencyTest.finishCallConcurrently_shouldChargeOnlyOnce, SecurityIntegrationTests., TransactionServiceTest.payForCall_shouldAllowNegativeBalance_whenClientRunsOutOfFundsDuringCall, TransactionServiceTest.payForCall_shouldTransferMoneyFromClientToTranslator, TransactionServiceTest.topUp_shouldIncreaseBalance_whenAmountIsPositive, WithdrawalRequestServiceTest.approve_shouldChangeStatusToApproved, WithdrawalRequestServiceTest.create_shouldReserveFunds_whenAmountIsValid, WithdrawalRequestServiceTest.reject_shouldRefundBalance

         - @ConditionalOnClass did not find required class 'org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties' (OnClassCondition)

   XADataSourceAutoConfiguration:
      Did not match:
         - @ConditionalOnBean (types: org.springframework.boot.jdbc.XADataSourceWrapper; SearchStrategy: all) did not find any beans of type org.springframework.boot.jdbc.XADataSourceWrapper (OnBeanCondition)
      Matched:
         - @ConditionalOnClass found required classes 'javax.sql.DataSource', 'jakarta.transaction.TransactionManager', 'org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType' (OnClassCondition)


Exclusions:
-----------

    None


Unconditional classes:
----------------------

    org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration

    org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration

    org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration

    org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration

    org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration

    org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration

    org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration



Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (C:\Users\span4\.m2\repository\net\bytebuddy\byte-buddy-agent\1.18.10\byte-buddy-agent-1.18.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

## judge
```
[PASS] 1. Есть unit-тест JwtAuthorizationListenerTest (Mockito, без Spring и базы), он зелёный — src/test/groovy/org/morago/config/JwtAuthorizationListenerTest.java — @ExtendWith(MockitoExtension.class), @Mock/@InjectMocks, без Spring. build_tests PASS: тестов 18 (было 12), +6 новых, новых красных нет.
[PASS] 2. Тест проверяет пять случаев: заголовок, ?token= без заголовка, нет ничего, refresh в ?token=, BLOCKED с ?token= — Тесты shouldAuthorize_whenValidTokenInHeader и shouldAuthorize_whenValidTokenInQueryParamAndNoHeader проверяют isAuthorized и userId=42. shouldReject_whenNoHeaderAndNoQueryParam проверяет отказ. shouldReject_whenRefreshTokenInQueryParam: пользователь ACTIVE, тип refresh, так что отказ даёт только проверка типа токена. shouldReject_whenBlockedUserWithTokenInQueryParam: тип access, так что отказ даёт только статус BLOCKED.
[PASS] 3. Если есть и заголовок, и параметр — используется заголовок — JwtAuthorizationListener.java:58-64 — extractToken сначала берёт Bearer-заголовок и только потом параметр. Тест shouldUseHeader_whenBothHeaderAndQueryParamPresent делает verify(jwtService).extractUsername("header-token") и never() для "query-token".
[PASS] 4. Остальное поведение слушателя не изменилось, новых красных тестов нет — Diff касается только получения токена (строки 26-29 и 56-64). Проверки подписи, типа access, существования пользователя и BLOCKED на строках 30-53 не тронуты. build_tests: красных 8, все были красными на main.
[UNVERIFIABLE] Чек-лист: логика проверки токена не продублирована — сначала достаём токен, потом одна проверка — Это пункт для человека. По коду он выполнен: extractToken (строка 58), затем один общий блок проверки (строки 30-53).
заметка: [мелочь] Добавлено условие token.isBlank() (строка 27), которого раньше не было. Пустой токен теперь отсекается до разбора. Поведение безопасное, но это небольшая добавка сверх задания.
заметка: [мелочь] Если заголовок Authorization есть, но не в формате Bearer, берётся ?token=. Это разумно, но тестом не закреплено.
```

## Новые файлы

- [src/test/groovy/org/morago/config/JwtAuthorizationListenerTest.java](../../src/test/groovy/org/morago/config/JwtAuthorizationListenerTest.java)

## Чек-лист для глаз (проверяет человек)
- [ ] Логика проверки токена не продублирована для двух способов — сначала достаём токен, потом одна проверка.
