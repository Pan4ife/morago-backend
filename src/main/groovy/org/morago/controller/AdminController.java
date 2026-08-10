package org.morago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.morago.dto.admin.RejectRequest;
import org.morago.model.TranslatorProfile;
import org.morago.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    @PatchMapping("/users/{id}/block")
    public ResponseEntity<String> block(@PathVariable Long id){
        adminService.blockUser(id);
        return ResponseEntity.ok("User was blocked");
    }

    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<String> unblock(@PathVariable Long id){
        adminService.unblockUser(id);
        return ResponseEntity.ok("User was unblocked");
    }

    @GetMapping("/translator-profiles/pending")
    public ResponseEntity<Page<TranslatorProfile>> getPendingTranslators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
        ){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getPendingTranslatorProfiles(pageable));

    }

    @PatchMapping("/translator-profiles/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable Long id){
        adminService.approveTranslator(id);
        return ResponseEntity.ok("Translator approved");
    }

    @PatchMapping("/translator-profiles/{id}/reject")
    public ResponseEntity<String> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request)
    {
        adminService.rejectTranslator(id, request.reason());
        return ResponseEntity.ok("Translator rejected");
    }
}
