package org.morago.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import io.jsonwebtoken.security.SignatureException;
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
import static org.mockito.Mockito.*;

/** Unit-тест авторизации подключения к сигналингу: токен из заголовка или из параметра ?token=. */
@ExtendWith(MockitoExtension.class)
public class JwtAuthorizationListenerTest {

    private static final String EMAIL = "user@morago.org";
    private static final Long USER_ID = 42L;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HandshakeData handshakeData;

    @InjectMocks
    private JwtAuthorizationListener listener;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new DefaultHttpHeaders();
        when(handshakeData.getHttpHeaders()).thenReturn(headers);
    }

    @Test
    void validAccessTokenInHeader_isAuthorizedWithUserId() {
        headers.set("Authorization", "Bearer header-token");
        givenToken("header-token", "access");
        givenUser(UserStatus.ACTIVE);

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertTrue(result.isAuthorized());
        assertEquals(USER_ID, result.getStoreParams().get("userId"));
    }

    @Test
    void validAccessTokenInQueryParam_withoutHeader_isAuthorizedWithUserId() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn("query-token");
        givenToken("query-token", "access");
        givenUser(UserStatus.ACTIVE);

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertTrue(result.isAuthorized());
        assertEquals(USER_ID, result.getStoreParams().get("userId"));
    }

    @Test
    void noHeaderAndNoQueryParam_isRejected() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn(null);

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void refreshTokenInQueryParam_isRejected() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn("refresh-token");
        givenToken("refresh-token", "refresh");

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
    }

    @Test
    void blockedUserWithTokenInQueryParam_isRejected() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn("query-token");
        givenToken("query-token", "access");
        givenUser(UserStatus.BLOCKED);

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
    }

    @Test
    void tokenWithInvalidSignatureInQueryParam_isRejected() {
        when(handshakeData.getSingleUrlParam("token")).thenReturn("forged-token");
        when(jwtService.extractUsername("forged-token")).thenThrow(new SignatureException("bad signature"));

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertFalse(result.isAuthorized());
        verifyNoInteractions(userRepository);
    }

    @Test
    void headerAndQueryParamBothPresent_headerTokenIsUsed() {
        headers.set("Authorization", "Bearer header-token");
        lenient().when(handshakeData.getSingleUrlParam("token")).thenReturn("query-token");
        givenToken("header-token", "access");
        givenUser(UserStatus.ACTIVE);

        AuthorizationResult result = listener.getAuthorizationResult(handshakeData);

        assertTrue(result.isAuthorized());
        verify(jwtService).extractUsername("header-token");
        verify(jwtService, never()).extractUsername("query-token");
    }

    private void givenToken(String token, String type) {
        when(jwtService.extractUsername(token)).thenReturn(EMAIL);
        when(jwtService.extractTokenType(token)).thenReturn(type);
    }

    private void givenUser(UserStatus status) {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setStatus(status);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }
}
