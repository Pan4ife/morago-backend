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
    public ResponseEntity<List<CallResponse>> getAll() {

        return ResponseEntity.ok(callService.getAll());

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
    public ResponseEntity<String> delete(@PathVariable Long id) {

        callService.delete(id);

        return ResponseEntity.ok("Call deleted");

    }

}
