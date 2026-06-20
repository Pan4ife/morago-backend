package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.auth.JwtResponse;
import org.morago.dto.auth.LoginRequest;
import org.morago.dto.auth.RefreshRequest;
import org.morago.dto.auth.RegisterRequest;

import org.morago.model.User;
import org.morago.repository.UserRepository;
import org.morago.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();

        user.setEmail(request.getEmail());

        user.setPassword(passwordEncoder.encode(request.getPassword()));

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

        return new JwtResponse(accessToken, refreshToken);

    }

    public JwtResponse refresh(RefreshRequest request) {

        String username = jwtService.extractUsername(
                request.getRefreshToken()
            );

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = jwtService.generateRefreshToken(user);

        return new JwtResponse(accessToken, refreshToken);
    }

}
