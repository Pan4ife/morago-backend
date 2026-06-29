package org.morago.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
