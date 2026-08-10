package org.morago.service;

import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import org.morago.exception.ResourceNotFoundException;
import org.morago.model.*;
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

    public void blockUser(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        }

    public void unblockUser(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public Page<TranslatorProfile> getPendingTranslatorProfiles(Pageable pageable){
        return translatorProfileRepository.findByStatus(VerificationStatus.PENDING, pageable);
    }

    @Transactional
    public void approveTranslator(Long id){
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
    }

    @Transactional
    public void rejectTranslator(Long id, String reason){
        TranslatorProfile translatorProfile = translatorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found"));
        translatorProfile.setStatus(VerificationStatus.REJECTED);
        translatorProfile.setReason(reason);
        translatorProfileRepository.save(translatorProfile);
    }
}
