package org.morago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.topic.TopicRequest;
import org.morago.dto.topic.TopicResponse;
import org.morago.service.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Темы",
        description = "Справочник тем перевода. Просмотр доступен всем, изменение — только администратору")
@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @Operation(summary = "Получить список всех тем",
            description = "Доступно без авторизации. Пагинация не применяется — список тем предполагается небольшим")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список тем успешно возвращён")
    })
    @GetMapping
    public ResponseEntity<List<TopicResponse>> getAll() {

        return ResponseEntity.ok(topicService.getAll());
    }

    @Operation(summary = "Создать новую тему",
            description = "Требуется роль ADMIN. Известная проблема: поле name не имеет валидации, допускается пустое значение")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Тема успешно создана"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicResponse> create(
        @Valid @RequestBody TopicRequest request) {

        return ResponseEntity.ok(topicService.create(request)
        );
    }

    @Operation(summary = "Удалить тему",
            description = "Требуется роль ADMIN. Известная проблема: при несуществующем id возвращается 500 вместо 404 (нет проверки существования перед удалением)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Тема успешно удалена"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "500", description = "Тема с указанным id не найдена (см. описание метода)")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        topicService.delete(id);

        return ResponseEntity.ok("Topic deleted");

    }

}
