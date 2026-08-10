package org.morago.service;

import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.morago.dto.auth.JwtResponse;
import org.morago.dto.auth.LoginRequest;
import org.morago.dto.auth.RefreshRequest;
import org.morago.dto.auth.RegisterRequest;

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

        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Current email already exists");
        }

        User user = new User();

        user.setEmail(normalizedEmail);

        user.setPassword(passwordEncoder.encode(request.password()));

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Role USER not found")
                );

        user.setRoles(Set.of(userRole));

        userRepository.save(user);
    }

    @Transactional
    public JwtResponse login(LoginRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {

            throw new BadCredentialsException("Wrong password");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {

            throw new BadCredentialsException("Account is blocked");
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

    @Transactional
    public JwtResponse refresh(RefreshRequest request) {

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        String tokenType = jwtService.extractTokenType(refreshTokenEntity.getToken());

        if (!tokenType.equals("refresh")) {
            throw new ConflictException("Invalid token type");
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        String username = jwtService.extractUsername(
                refreshTokenEntity.getToken()
        );

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

    @Transactional
    public void logout(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        refreshTokenRepository.deleteByUser(user);
    }

}
