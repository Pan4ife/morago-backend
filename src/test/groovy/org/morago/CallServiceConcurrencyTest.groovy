package org.morago

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.morago.model.*;
import org.morago.repository.CallRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository
import org.morago.service.CallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CallServiceConcurrencyTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        // свой контекст Spring — свой Socket.IO-сервер: порт 0 (любой свободный), чтобы не занять 9092
        // у контекста SecurityIntegrationTests, который живёт в том же прогоне
        registry.add("socketio.port", () -> 0);
    }

    @Autowired
    private CallService callService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TranslatorProfileRepository translatorProfileRepository;

    @Autowired
    private CallRepository callRepository;

    private User client;
    private User translatorUser;
    private TranslatorProfile translatorProfile;
    private Call call;

    @BeforeEach
    void setUp() {

        client = new User();
        client.setEmail("concurrent-client@test.com");
        client.setPassword("password");
        client.setBalance(BigDecimal.valueOf(10000));
        userRepository.save(client);

        translatorUser = new User();
        translatorUser.setEmail("concurrent-translator@test.com");
        translatorUser.setPassword("password");
        translatorUser.setBalance(BigDecimal.ZERO);
        userRepository.save(translatorUser);

        translatorProfile = new TranslatorProfile();
        translatorProfile.setUser(translatorUser);
        translatorProfile.setHourlyRate(BigDecimal.valueOf(600));
        translatorProfile.setCreatedAt(LocalDateTime.now());
        translatorProfile.setUpdatedAt(LocalDateTime.now());
        translatorProfileRepository.save(translatorProfile);

        call = new Call();
        call.setClient(client);
        call.setTranslator(translatorProfile);
        call.setStatus(CallStatus.IN_PROGRESS);
        call.setStartTime(LocalDateTime.now().minusMinutes(5));
        call.setCost(BigDecimal.ZERO);
        callRepository.save(call);
    }

    @Test
    void finishCallConcurrently_shouldChargeOnlyOnce() throws InterruptedException {

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // все потоки стартуют одновременно
                    callService.finish(call.getId(), translatorUser.getEmail());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        readyLatch.await();       // ждём, пока оба потока будут готовы
        startLatch.countDown();   // отпускаем оба одновременно
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Проверка 1: ровно один поток успешно завершил звонок
        assertEquals(1, successCount.get(), "Только один вызов finish() должен быть успешным");
        assertEquals(1, failureCount.get(), "Второй вызов должен завершиться ошибкой");

        // Проверка 2: баланс клиента списан только один раз
        User updatedClient = userRepository.findById(client.getId()).orElseThrow();
        BigDecimal expectedCost = BigDecimal.valueOf(50).setScale(2); // 5 минут * (600/60) = 50
        assertEquals(0, updatedClient.getBalance().compareTo(BigDecimal.valueOf(10000).subtract(expectedCost)));

        // Проверка 3: переводчику начислено только один раз
        User updatedTranslator = userRepository.findById(translatorUser.getId()).orElseThrow();
        assertEquals(0, updatedTranslator.getBalance().compareTo(expectedCost));
    }



}
