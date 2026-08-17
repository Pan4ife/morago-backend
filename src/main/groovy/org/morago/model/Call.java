package org.morago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "calls")
@Getter
@Setter
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

        @Column(precision = 19, scale = 2)
        private BigDecimal cost;

        private Long durationSeconds;

        @Enumerated(EnumType.STRING)
        private CallStatus status;

        @CreationTimestamp
        private LocalDateTime createdAt;

        @UpdateTimestamp
        private LocalDateTime updatedAt;

        @OneToOne(mappedBy = "call")
        private Review review;


}