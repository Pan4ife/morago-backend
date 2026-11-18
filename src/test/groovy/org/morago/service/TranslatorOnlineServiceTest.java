package org.morago.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.model.VerificationStatus;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit-тесты включения и выключения статуса «На связи» в TranslatorProfileService. */
@ExtendWith(MockitoExtension.class)
public class TranslatorOnlineServiceTest {

    private static final String EMAIL = "translator@test.com";

    @Mock
    private TranslatorProfileRepository translatorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TranslatorProfileService translatorProfileService;

    private TranslatorProfile givenProfile(VerificationStatus status, boolean online) {
        User user = new User();
        user.setEmail(EMAIL);
        TranslatorProfile profile = new TranslatorProfile();
        profile.setId(5L);
        profile.setUser(user);
        profile.setStatus(status);
        profile.setOnline(online);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(translatorProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        return profile;
    }

    @Test
    void updateOnline_verifiedGoesOnline_savedAndReturned() {
        TranslatorProfile profile = givenProfile(VerificationStatus.VERIFIED, false);
        when(translatorProfileRepository.save(profile)).thenReturn(profile);

        TranslatorProfileResponse response = translatorProfileService.updateOnline(EMAIL, true);

        verify(translatorProfileRepository).save(profile);
        assertTrue(profile.isOnline());
        assertTrue(response.online());
        assertEquals(EMAIL, response.email());
    }

    @Test
    void updateOnline_pendingGoesOnline_conflictAndNothingSaved() {
        TranslatorProfile profile = givenProfile(VerificationStatus.PENDING, false);

        assertThrows(ConflictException.class,
                () -> translatorProfileService.updateOnline(EMAIL, true));

        verify(translatorProfileRepository, never()).save(any());
        assertFalse(profile.isOnline());
    }

    @ParameterizedTest
    @EnumSource(VerificationStatus.class)
    void updateOnline_anyStatusGoesOffline_saved(VerificationStatus status) {
        TranslatorProfile profile = givenProfile(status, true);
        when(translatorProfileRepository.save(profile)).thenReturn(profile);

        TranslatorProfileResponse response = translatorProfileService.updateOnline(EMAIL, false);

        verify(translatorProfileRepository).save(profile);
        assertFalse(profile.isOnline());
        assertFalse(response.online());
    }

    @Test
    void updateOnline_noProfile_notFound() {
        User user = new User();
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(translatorProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> translatorProfileService.updateOnline(EMAIL, true));

        verify(translatorProfileRepository, never()).save(any());
    }
}
