package org.morago.repository;

import org.morago.model.Transaction;

import org.morago.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByCallIdAndType(Long callId, TransactionType type);


}
