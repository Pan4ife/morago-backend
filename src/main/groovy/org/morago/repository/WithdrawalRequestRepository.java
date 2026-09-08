package org.morago.repository;

import org.morago.model.WithdrawalRequest;
import org.morago.model.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    Page<WithdrawalRequest> findByTranslatorIdOrderByCreatedAtDesc(Long translatorId, Pageable pageable);

    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);
}
