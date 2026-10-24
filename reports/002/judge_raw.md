```json
{
  "verdict": "PASS",
  "criteria": [
    {
      "criterion": "1. Есть unit-тест JwtAuthorizationListenerTest (Mockito, без Spring и базы), он зелёный",
      "status": "PASS",
      "evidence": "src/test/groovy/org/morago/config/JwtAuthorizationListenerTest.java — @ExtendWith(MockitoExtension.class), @Mock/@InjectMocks, без Spring. build_tests PASS: тестов 18 (было 12), +6 новых, новых красных нет."
    },
    {
      "criterion": "2. Тест проверяет пять случаев: заголовок, ?token= без заголовка, нет ничего, refresh в ?token=, BLOCKED с ?token=",
      "status": "PASS",
      "evidence": "Тесты shouldAuthorize_whenValidTokenInHeader и shouldAuthorize_whenValidTokenInQueryParamAndNoHeader проверяют isAuthorized и userId=42. shouldReject_whenNoHeaderAndNoQueryParam проверяет отказ. shouldReject_whenRefreshTokenInQueryParam: пользователь ACTIVE, тип refresh, так что отказ даёт только проверка типа токена. shouldReject_whenBlockedUserWithTokenInQueryParam: тип access, так что отказ даёт только статус BLOCKED."
    },
    {
      "criterion": "3. Если есть и заголовок, и параметр — используется заголовок",
      "status": "PASS",
      "evidence": "JwtAuthorizationListener.java:58-64 — extractToken сначала берёт Bearer-заголовок и только потом параметр. Тест shouldUseHeader_whenBothHeaderAndQueryParamPresent делает verify(jwtService).extractUsername(\"header-token\") и never() для \"query-token\"."
    },
    {
      "criterion": "4. Остальное поведение слушателя не изменилось, новых красных тестов нет",
      "status": "PASS",
      "evidence": "Diff касается только получения токена (строки 26-29 и 56-64). Проверки подписи, типа access, существования пользователя и BLOCKED на строках 30-53 не тронуты. build_tests: красных 8, все были красными на main."
    },
    {
      "criterion": "Чек-лист: логика проверки токена не продублирована — сначала достаём токен, потом одна проверка",
      "status": "UNVERIFIABLE",
      "evidence": "Это пункт для человека. По коду он выполнен: extractToken (строка 58), затем один общий блок проверки (строки 30-53)."
    }
  ],
  "issues": [],
  "notes": [
    "[мелочь] Добавлено условие token.isBlank() (строка 27), которого раньше не было. Пустой токен теперь отсекается до разбора. Поведение безопасное, но это небольшая добавка сверх задания.",
    "[мелочь] Если заголовок Authorization есть, но не в формате Bearer, берётся ?token=. Это разумно, но тестом не закреплено."
  ]
}
```