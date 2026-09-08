package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.review.ReviewRequest;
import org.morago.dto.review.ReviewResponse;
import org.morago.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Отзывы",
        description = "Создание и просмотр отзывов клиентов о переводчиках после завершённых звонков")
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Получить список всех отзывов",
            description = "Возвращает пагинированный список всех отзывов без фильтрации по пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список отзывов успешно возвращён")
    })
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAll(pageable));
    }

    @Operation(summary = "Оставить отзыв о звонке",
            description = "Доступно только клиенту завершённого звонка. Один отзыв на звонок; оставление отзыва пересчитывает средний рейтинг переводчика")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отзыв успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации (rating вне диапазона 1–5, пустой комментарий и т.д.)"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является клиентом этого звонка"),
            @ApiResponse(responseCode = "404", description = "Звонок с указанным id не найден"),
            @ApiResponse(responseCode = "409", description = "Звонок ещё не завершён, либо отзыв уже существует")
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            Authentication authentication,
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(reviewService.create(authentication.getName(), request)
        );
    }

    @Operation(summary = "Удалить отзыв",
            description = "Доступно клиенту, оставившему отзыв, либо администратору. Удаление пересчитывает средний рейтинг переводчика")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отзыв успешно удалён"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён — пользователь не автор отзыва и не админ"),
            @ApiResponse(responseCode = "404", description = "Отзыв с указанным id не найден")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        reviewService.delete(id, authentication.getName());

        return ResponseEntity.ok("Review deleted");
    }
}
