package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.auth.JwtResponse;
import org.morago.dto.auth.LoginRequest;
import org.morago.dto.auth.RefreshRequest;
import org.morago.dto.auth.RegisterRequest;

import org.morago.model.RefreshToken;
import org.morago.model.Role;
import org.morago.model.RoleName;
import org.morago.model.User;
import org.morago.repository.RefreshTokenRepository;
import org.morago.repository.RoleRepository;
import org.morago.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final RoleRepository roleRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = new User();

        user.setEmail(request.getEmail());

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByName(RoleName.USER)
                        .orElseThrow(
                                () -> new RuntimeException("Role USER not found")
                        );

        user.setRoles(Set.of(userRole));

        userRepository.save(user);
    }

    public JwtResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new BadCredentialsException("Wrong password");
        }

        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshTokenEntity = new RefreshToken();

        refreshTokenEntity.setToken(refreshToken);

        refreshTokenEntity.setUser(user);

        refreshTokenEntity.setCreatedAt(LocalDateTime.now());

        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(30));

        refreshTokenRepository.save(refreshTokenEntity);

        return new JwtResponse(accessToken, refreshToken);

    }

    public JwtResponse refresh(RefreshRequest request) {

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        String tokenType = jwtService.extractTokenType(refreshTokenEntity.getToken());

    if (!tokenType.equals("refresh")) {
        throw new RuntimeException("Invalid token type");
    }

        String username = jwtService.extractUsername(
                refreshTokenEntity.getToken()
            );

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.deleteByUser(user);

        RefreshToken newRefreshTokenEntity = new RefreshToken();

        newRefreshTokenEntity.setToken(refreshToken);

        newRefreshTokenEntity.setUser(user);

        newRefreshTokenEntity.setCreatedAt(LocalDateTime.now());

        newRefreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(30));

        refreshTokenRepository.save(newRefreshTokenEntity);

        return new JwtResponse(accessToken, refreshToken);
    }

    public void logout(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        refreshTokenRepository.deleteByUser(user);
    }

}
