package org.morago.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.model.VerificationStatus;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit-тест DemoDataInitializer: анкета демо-переводчика и баланс демо-клиента. */
@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TranslatorProfileRepository translatorProfileRepository;
    @InjectMocks
    private DemoDataInitializer initializer;

    private User user(String email, BigDecimal balance) {
        User user = new User();
        user.setEmail(email);
        user.setBalance(balance);
        return user;
    }

    @Test
    void createsVerifiedProfileWhenTranslatorHasNone() {
        User translator = user(DemoDataInitializer.TRANSLATOR_EMAIL, BigDecimal.ZERO);
        when(userRepository.findByEmail(DemoDataInitializer.TRANSLATOR_EMAIL)).thenReturn(Optional.of(translator));
        when(translatorProfileRepository.findByUser(translator)).thenReturn(Optional.empty());

        initializer.createTranslatorProfile();

        ArgumentCaptor<TranslatorProfile> captor = ArgumentCaptor.forClass(TranslatorProfile.class);
        verify(translatorProfileRepository).save(captor.capture());
        TranslatorProfile saved = captor.getValue();
        assertSame(translator, saved.getUser());
        assertEquals(VerificationStatus.VERIFIED, saved.getStatus());
        assertEquals(0, BigDecimal.valueOf(600).compareTo(saved.getHourlyRate()));
        assertEquals("Демо-переводчик", saved.getBio());
        assertNotNull(saved.getVerifiedAt());
        assertEquals("demo-data", saved.getVerifiedBy());
    }

    @Test
    void doesNotTouchExistingProfile() {
        User translator = user(DemoDataInitializer.TRANSLATOR_EMAIL, BigDecimal.ZERO);
        when(userRepository.findByEmail(DemoDataInitializer.TRANSLATOR_EMAIL)).thenReturn(Optional.of(translator));
        when(translatorProfileRepository.findByUser(translator)).thenReturn(Optional.of(new TranslatorProfile()));

        initializer.createTranslatorProfile();

        verify(translatorProfileRepository, never()).save(any());
    }

    @Test
    void setsBalanceWhenZero() {
        User client = user(DemoDataInitializer.CLIENT_EMAIL, BigDecimal.ZERO);
        when(userRepository.findByEmail(DemoDataInitializer.CLIENT_EMAIL)).thenReturn(Optional.of(client));

        initializer.grantClientBalance();

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(client.getBalance()));
        verify(userRepository).save(client);
    }

    @Test
    void keepsNonZeroBalance() {
        User client = user(DemoDataInitializer.CLIENT_EMAIL, BigDecimal.valueOf(500));
        when(userRepository.findByEmail(DemoDataInitializer.CLIENT_EMAIL)).thenReturn(Optional.of(client));

        initializer.grantClientBalance();

        assertEquals(0, BigDecimal.valueOf(500).compareTo(client.getBalance()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenUsersMissing() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> initializer.run());

        verify(userRepository, never()).save(any());
        verify(translatorProfileRepository, never()).save(any());
    }
}
