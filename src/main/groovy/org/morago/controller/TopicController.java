package org.morago.controller;

import lombok.RequiredArgsConstructor;


import org.morago.dto.topic.TopicRequest;
import org.morago.dto.topic.TopicResponse;
import org.morago.service.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ResponseEntity<List<TopicResponse>> getAll() {

        return ResponseEntity.ok(topicService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicResponse> create(
        @RequestBody TopicRequest request) {

        return ResponseEntity.ok(topicService.create(request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        topicService.delete(id);

        return ResponseEntity.ok("Topic deleted");

    }

}
