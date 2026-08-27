package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.admin.PendingTranslatorResponse;
import org.morago.dto.admin.RejectRequest;
import org.morago.dto.admin.UserPageResponse;
import org.morago.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserPageResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/translator-profiles/pending")
    public ResponseEntity<Page<PendingTranslatorResponse>> getPendingTranslators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getPendingTranslatorProfiles(pageable));

    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<String> block(@PathVariable Long id, Authentication authentication){
        String adminEmail  = authentication.getName();
        adminService.blockUser(id, adminEmail);
        return ResponseEntity.ok("User was blocked");
    }

    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<String> unblock(@PathVariable Long id, Authentication authentication){
        String adminEmail = authentication.getName();
        adminService.unblockUser(id, adminEmail);
        return ResponseEntity.ok("User was unblocked");
    }

    @PatchMapping("/translator-profiles/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable Long id, Authentication authentication){
        String adminEmail = authentication.getName();
        adminService.approveTranslator(id, adminEmail);
        return ResponseEntity.ok("Translator approved");
    }

    @PatchMapping("/translator-profiles/{id}/reject")
    public ResponseEntity<String> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            Authentication authentication
    )
    {
        String adminEmail = authentication.getName();
        adminService.rejectTranslator(id, request.reason(), adminEmail);
        return ResponseEntity.ok("Translator rejected");
    }
}
