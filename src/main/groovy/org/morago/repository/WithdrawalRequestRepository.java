package org.morago.repository;

import org.morago.model.WithdrawalRequest;
import org.morago.model.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByTranslatorIdOrderByCreatedAtDesc(Long translatorId);

    List<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status);
}
