package org.morago.security;

import lombok.RequiredArgsConstructor;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.model.VerificationStatus;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Демо-данные профиля dev: анкета переводчика VERIFIED и баланс клиенту, чтобы проверить звонок руками. */
@Component
@RequiredArgsConstructor
@Profile("dev")
public class DemoDataInitializer {
    static final String TRANSLATOR_EMAIL = "translator@morago.com";
    static final String CLIENT_EMAIL = "user@morago.com";
    static final BigDecimal DEMO_HOURLY_RATE = BigDecimal.valueOf(600);
    static final BigDecimal DEMO_BALANCE = BigDecimal.valueOf(1000);

    private final UserRepository userRepository;
    private final TranslatorProfileRepository translatorProfileRepository;

    // ApplicationReadyEvent публикуется после всех CommandLineRunner, значит после DataInitializer.
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        createTranslatorProfile();
        grantClientBalance();
    }

    void createTranslatorProfile() {
        userRepository.findByEmail(TRANSLATOR_EMAIL)
                .filter(user -> translatorProfileRepository.findByUser(user).isEmpty())
                .ifPresent(user -> {
                    LocalDateTime now = LocalDateTime.now();
                    TranslatorProfile profile = new TranslatorProfile();
                    profile.setUser(user);
                    profile.setStatus(VerificationStatus.VERIFIED);
                    profile.setHourlyRate(DEMO_HOURLY_RATE);
                    profile.setBio("Демо-переводчик");
                    profile.setVerifiedAt(now);
                    profile.setVerifiedBy("demo-data");
                    profile.setCreatedAt(now);
                    profile.setUpdatedAt(now);
                    translatorProfileRepository.save(profile);
                });
    }

    void grantClientBalance() {
        userRepository.findByEmail(CLIENT_EMAIL)
                .filter(user -> user.getBalance() == null || user.getBalance().signum() == 0)
                .ifPresent(user -> {
                    user.setBalance(DEMO_BALANCE);
                    userRepository.save(user);
                });
    }
}
