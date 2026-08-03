package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.service.CallService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @GetMapping
    public ResponseEntity<List<CallResponse>> getAll(Authentication authentication) {

        return ResponseEntity.ok(callService.getAll(authentication.getName()));

    }

    @PostMapping
    public ResponseEntity<CallResponse> create(Authentication authentication,
                                               @Valid @RequestBody CallRequest request) {

        return ResponseEntity.ok(
                callService.create(
                        authentication.getName(),
                        request
                )
        );

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        callService.delete(
                        id,
                        authentication.getName()
        );

        return ResponseEntity.ok("Call deleted");

    }

    @PatchMapping("/{id}/finish")
    public ResponseEntity<CallResponse> finish(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.finish(
                id,
                authentication.getName())
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<CallResponse> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.cancel(
                id,
                authentication.getName())
        );
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<CallResponse> start(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.start(
                id,
                authentication.getName())
        );
    }

}
