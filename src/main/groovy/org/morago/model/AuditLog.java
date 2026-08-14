package org.morago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit")
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long adminId;

    @Column
    @Enumerated(EnumType.STRING)
    private AuditActionType actionType;

    private Long targetId;

    @CreationTimestamp
    private LocalDateTime timestamp;

    private String reason;
}
