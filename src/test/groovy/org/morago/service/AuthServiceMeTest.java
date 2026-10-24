package org.morago.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.dto.auth.MeResponse;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.Role;
import org.morago.model.RoleName;
import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.morago.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceMeTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void me_shouldReturnIdRolesBalanceAndTranslatorProfileId() {
        Role role = new Role();
        role.setName(RoleName.TRANSLATOR);
        TranslatorProfile profile = new TranslatorProfile();
        profile.setId(5L);
        User user = new User();
        user.setId(3L);
        user.setEmail("t@test.com");
        user.setBalance(BigDecimal.valueOf(42));
        user.getRoles().add(role);
        user.setTranslatorProfile(profile);
        when(userRepository.findByEmail("t@test.com")).thenReturn(Optional.of(user));

        MeResponse me = authService.me("t@test.com");

        assertEquals(3L, me.id());
        assertEquals("t@test.com", me.email());
        assertEquals(Set.of("TRANSLATOR"), me.roles());
        assertEquals(BigDecimal.valueOf(42), me.balance());
        assertEquals(5L, me.translatorProfileId());
    }

    @Test
    void me_shouldReturnNullProfileId_whenUserIsNotTranslator() {
        User user = new User();
        user.setId(4L);
        user.setEmail("c@test.com");
        when(userRepository.findByEmail("c@test.com")).thenReturn(Optional.of(user));

        assertNull(authService.me("c@test.com").translatorProfileId());
    }

    @Test
    void me_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("x@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.me("x@test.com"));
    }
}
