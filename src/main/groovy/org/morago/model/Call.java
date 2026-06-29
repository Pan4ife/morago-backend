package org.morago.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "calls")
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne
    @JoinColumn(name = "translator_id")
    private TranslatorProfile translator;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal cost;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
