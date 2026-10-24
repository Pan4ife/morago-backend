package org.morago.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morago.model.User;
import org.morago.model.UserStatus;
import org.morago.repository.UserRepository;
import org.morago.service.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class JwtAuthorizationListenerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HandshakeData handshakeData;

    @InjectMocks
    private JwtAuthorizationListener listener;

    private HttpHeaders headers;
    private User user;

    @BeforeEach
    void setUp() {
        headers = new DefaultHttpHeaders();
        lenient().when(handshakeData.getHttpHeaders()).thenReturn(headers);

        user = new User();
        user.setId(42L);
        user.setEmail("client@test.com");
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void shouldAuthorize_whenValidTokenInHeader() {
        headers.set("Authorization", "Bearer header-token");
        stubValidAccessToken("header-token");

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertTrue(result.isAuthorized());
        assertEquals(42L, result.getStoreParams().get("userId"));
    }

    @Test
    void shouldAuthorize_whenValidTokenInQueryParamAndNoHeader() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn("query-token");
        stubValidAccessToken("query-token");

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertTrue(result.isAuthorized());
        assertEquals(42L, result.getStoreParams().get("userId"));
    }

    @Test
    void shouldUseHeader_whenBothHeaderAndQueryParamPresent() {
        headers.set("Authorization", "Bearer header-token");
        lenient().when(handshakeData.getSingleUrlParam("token")).thenReturn("query-token");
        stubValidAccessToken("header-token");

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertTrue(result.isAuthorized());
        verify(jwtService).extractUsername("header-token");
        verify(jwtService, never()).extractUsername("query-token");
    }

    @Test
    void shouldReject_whenNoHeaderAndNoQueryParam() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn(null);

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void shouldReject_whenRefreshTokenInQueryParam() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn("refresh-token");
        when(jwtService.extractUsername("refresh-token")).thenReturn("client@test.com");
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));
        when(jwtService.extractTokenType("refresh-token")).thenReturn("refresh");

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
    }

    @Test
    void shouldReject_whenBlockedUserWithTokenInQueryParam() {
        user.setStatus(UserStatus.BLOCKED);
        when(handshakeData.getSingleUrlParam("token")).thenReturn("query-token");
        when(jwtService.extractUsername("query-token")).thenReturn("client@test.com");
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));
        lenient().when(jwtService.extractTokenType("query-token")).thenReturn("access");

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
    }

    private void stubValidAccessToken(String token) {
        when(jwtService.extractUsername(token)).thenReturn("client@test.com");
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(user));
        when(jwtService.extractTokenType(token)).thenReturn("access");
    }
}
