package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.admin.PendingTranslatorResponse;
import org.morago.exception.ForbiddenException;
import org.morago.exception.ConflictException;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
import org.morago.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final TranslatorProfileRepository translatorProfileRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public Page<PendingTranslatorResponse> getPendingTranslatorProfiles(Pageable pageable) {
        return translatorProfileRepository.findByStatus(VerificationStatus.PENDING, pageable)
                .map(this::mapToPendingTranslatorResponse);
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private void logAction(Long adminId, AuditActionType actionType, Long targetId, String reason) {
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setActionType(actionType);
        log.setTargetId(targetId);
        log.setReason(reason);
        auditLogRepository.save(log);
    }

    @Transactional
    public void blockUser(Long id, String adminEmail) {
        User admin = getCurrentUser(adminEmail);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(user.getId().equals(admin.getId())){
            throw new ForbiddenException("You cannot block YOURSELF");
        }
        user.setStatus(UserStatus.BLOCKED);
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);
        logAction(admin.getId(), AuditActionType.USER_BLOCKED, id, null);
    }

    @Transactional
    public void unblockUser(Long id, String adminEmail) {
        User admin = getCurrentUser(adminEmail);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        logAction(admin.getId(), AuditActionType.USER_UNBLOCKED, id, null);
    }

    @Transactional
    public void approveTranslator(Long id, String adminEmail) {
        User admin = getCurrentUser(adminEmail);
        TranslatorProfile translatorProfile = translatorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found"));
        if (translatorProfile.getStatus() != VerificationStatus.PENDING) {
            throw new ConflictException("Only PENDING profiles can be approved");
        }
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
    public void rejectTranslator(Long id, String reason, String adminEmail) {
        User admin = getCurrentUser(adminEmail);
        TranslatorProfile translatorProfile = translatorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found"));
        if (translatorProfile.getStatus() != VerificationStatus.PENDING &&
                translatorProfile.getStatus() != VerificationStatus.VERIFIED) {
            throw new ConflictException("Cannot reject already REJECTED profile");
        }
        if (translatorProfile.getStatus() == VerificationStatus.VERIFIED) {
            User user = translatorProfile.getUser();
            Role role = roleRepository.findByName(RoleName.TRANSLATOR)
                    .orElseThrow(() -> new ResourceNotFoundException("Role TRANSLATOR not found"));
            user.getRoles().remove(role);
            userRepository.save(user);
        }
        translatorProfile.setStatus(VerificationStatus.REJECTED);
        translatorProfile.setReason(reason);
        translatorProfileRepository.save(translatorProfile);
        logAction(admin.getId(), AuditActionType.TRANSLATOR_REJECTED, id, reason);
    }

    private PendingTranslatorResponse mapToPendingTranslatorResponse(TranslatorProfile translatorProfile) {
        Set<String> languages = translatorProfile.getLanguages()
                .stream()
                .map(Language::getName)
                .collect(Collectors.toSet());

        Set<String> topics = translatorProfile.getTopics()
                .stream()
                .map(Topic::getName)
                .collect(Collectors.toSet());

        return new PendingTranslatorResponse(
                translatorProfile.getId(),
                translatorProfile.getBio(),
                languages,
                topics,
                translatorProfile.getUser().getEmail()
        );
    }
}
