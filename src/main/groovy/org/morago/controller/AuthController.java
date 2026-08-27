package org.morago.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.morago.dto.auth.JwtResponse;
import org.morago.dto.auth.LoginRequest;
import org.morago.dto.auth.RefreshRequest;
import org.morago.dto.auth.RegisterRequest;
import org.morago.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Аутентификация",
        description = "Регистрация, вход, обновление и завершение сессии пользователя")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Регистрация нового пользователя",
            description = "Создаёт пользователя с ролью USER. Email нормализуется (trim + lowercase) перед проверкой на уникальность")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "400", description = "Некорректный формат email или слишком короткий пароль"),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request
            ) {

        authService.register(request);

        return ResponseEntity.ok("User created");
    }

    @Operation(summary = "Вход в систему",
            description = "Возвращает пару access/refresh токенов. Предыдущий refresh-токен пользователя удаляется")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход, возвращены токены"),
            @ApiResponse(responseCode = "400", description = "Некорректный формат email или пароля"),
            @ApiResponse(responseCode = "401", description = "Неверный пароль, либо аккаунт заблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным email не найден")
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Обновить пару токенов",
            description = "Принимает действующий refresh-токен и выдаёт новую пару access/refresh токенов; старый refresh-токен инвалидируется")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены"),
            @ApiResponse(responseCode = "400", description = "Refresh-токен не передан"),
            @ApiResponse(responseCode = "401", description = "Refresh-токен просрочен"),
            @ApiResponse(responseCode = "404", description = "Refresh-токен не найден"),
            @ApiResponse(responseCode = "409", description = "Передан токен неверного типа (не refresh)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Получить email текущего пользователя",
            description = "Возвращает email пользователя, соответствующего переданному access-токену")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email успешно возвращён")
    })

    @GetMapping("/me")
    public ResponseEntity<String> me(Authentication authentication) {


        return ResponseEntity.ok(authentication.getName());
    }

    @Operation(summary = "Выход из системы",
            description = "Удаляет refresh-токен текущего пользователя, делая его недействительным для дальнейшего обновления сессии")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Выход выполнен успешно"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {

        authService.logout(authentication.getName());

        return ResponseEntity.ok("Logged out");
    }
}
