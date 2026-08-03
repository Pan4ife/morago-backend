package org.morago.controller;


import jakarta.validation.Valid;
import org.morago.dto.auth.JwtResponse;
import org.morago.dto.auth.LoginRequest;
import org.morago.dto.auth.RefreshRequest;
import org.morago.dto.auth.RegisterRequest;
import org.morago.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request
            ) {

        authService.register(request);

        return ResponseEntity.ok("User created");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<String> me(Authentication authentication) {


        return ResponseEntity.ok(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {

        authService.logout(authentication.getName());

        return ResponseEntity.ok("Logged out");
    }
}
