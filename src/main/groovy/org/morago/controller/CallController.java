package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.call.CallRequest;
import org.morago.dto.call.CallResponse;
import org.morago.service.CallService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @GetMapping
    public ResponseEntity<Page<CallResponse>> getAll(Authentication authentication, Pageable pageable)
    {
        return ResponseEntity.ok(callService.getAll(authentication.getName(), pageable));

    }
    @GetMapping("/{id}")
    public ResponseEntity<CallResponse> getById(@PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(callService.getById(id, authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
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
    @PreAuthorize("hasRole('TRANSLATOR') or hasRole('ADMIN')")
    public ResponseEntity<CallResponse> finish(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.finish(
                id,
                authentication.getName())
        );
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CallResponse> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.cancel(
                id,
                authentication.getName())
        );
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('TRANSLATOR') or hasRole('ADMIN')")
    public ResponseEntity<CallResponse> start(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(callService.start(
                id,
                authentication.getName())
        );
    }

}
