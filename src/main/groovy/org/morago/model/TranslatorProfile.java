package org.morago.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "translator_profiles")
public class TranslatorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bio;

    private Double rating;

    private boolean isOnline;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "translator_languages",
            joinColumns = @JoinColumn(name = "translator_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    private Set<Language> languages = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "translator_topics",
            joinColumns = @JoinColumn(name = "translator_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();

}
