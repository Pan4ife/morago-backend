package org.morago.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.dto.translatorprofile.TranslatorProfileResponse;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.model.VerificationStatus;
import org.morago.repository.TranslatorProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TranslatorProfileServiceTest {

    @Mock
    private TranslatorProfileRepository translatorProfileRepository;

    @InjectMocks
    private TranslatorProfileService translatorProfileService;

    @Test
    void getVerified_shouldReturnOnlyVerifiedProfilesMappedToResponse() {
        User user = new User();
        user.setEmail("translator@test.com");
        TranslatorProfile profile = new TranslatorProfile();
        profile.setId(7L);
        profile.setUser(user);
        profile.setBio("EN-KO");
        profile.setOnline(true);
        profile.setHourlyRate(BigDecimal.valueOf(30));
        Pageable pageable = PageRequest.of(0, 10);
        when(translatorProfileRepository.findByStatus(VerificationStatus.VERIFIED, pageable))
                .thenReturn(new PageImpl<>(List.of(profile), pageable, 1));

        Page<TranslatorProfileResponse> result = translatorProfileService.getVerified(pageable);

        verify(translatorProfileRepository).findByStatus(VerificationStatus.VERIFIED, pageable);
        assertEquals(1, result.getTotalElements());
        TranslatorProfileResponse response = result.getContent().get(0);
        assertEquals(7L, response.id());
        assertEquals("translator@test.com", response.email());
        assertTrue(response.online());
        assertEquals(BigDecimal.valueOf(30), response.hourlyRate());
    }
}
