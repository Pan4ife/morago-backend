package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.AuditLogRepository;
import org.morago.repository.RoleRepository;
import org.morago.repository.TranslatorProfileRepository;
import org.morago.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final TranslatorProfileRepository translatorProfileRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;

    public Page<TranslatorProfile> getPendingTranslatorProfiles(Pageable pageable){
        return translatorProfileRepository.findByStatus(VerificationStatus.PENDING, pageable);
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    private void logAction(Long adminId, AuditActionType actionType, Long targetId, String reason){
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setActionType(actionType);
        log.setTargetId(targetId);
        log.setReason(reason);
        auditLogRepository.save(log);
    }

    @Transactional
    public void blockUser(Long id, String adminEmail){
        User admin = getCurrentUser(adminEmail);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        logAction(admin.getId(), AuditActionType.USER_BLOCKED, id, null);
        }

    @Transactional
    public void unblockUser(Long id, String adminEmail){
        User admin = getCurrentUser(adminEmail);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        logAction(admin.getId(), AuditActionType.USER_UNBLOCKED, id, null);
    }

    @Transactional
    public void approveTranslator(Long id, String adminEmail){
        User admin = getCurrentUser(adminEmail);
        TranslatorProfile translatorProfile = translatorProfileRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found"));
        translatorProfile.setStatus(VerificationStatus.VERIFIED);
        translatorProfile.setVerifiedAt(LocalDateTime.now());
        Role role = roleRepository.findByName(RoleName.TRANSLATOR)
                        .orElseThrow(() -> new ResourceNotFoundException("Role USER not found"));
        User user = translatorProfile.getUser();
        user.getRoles().add(role);
        userRepository.save(user);
        translatorProfileRepository.save(translatorProfile);
        logAction(admin.getId(), AuditActionType.TRANSLATOR_APPROVED, id, null);

    }

    @Transactional
    public void rejectTranslator(Long id, String reason, String adminEmail){
        User admin = getCurrentUser(adminEmail);
        TranslatorProfile translatorProfile = translatorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found"));
        translatorProfile.setStatus(VerificationStatus.REJECTED);
        translatorProfile.setReason(reason);
        translatorProfileRepository.save(translatorProfile);
        logAction(admin.getId(), AuditActionType.TRANSLATOR_REJECTED, id, null);
    }
}
