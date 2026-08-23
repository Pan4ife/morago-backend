package org.morago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
@Getter
@Setter
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "translator_id", nullable = false)
    private User translator;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Column(name = "transaction_id")
    private Long transactionId;
}
