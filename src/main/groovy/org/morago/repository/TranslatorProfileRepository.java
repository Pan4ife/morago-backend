package org.morago.repository;

import org.morago.model.TranslatorProfile;
import org.morago.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranslatorProfileRepository extends JpaRepository<TranslatorProfile, Long> {

    Optional<TranslatorProfile> findByUser(User user);
}
