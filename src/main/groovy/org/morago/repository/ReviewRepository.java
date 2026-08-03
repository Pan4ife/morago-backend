package org.morago.repository;

import org.morago.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
    SELECT AVG(r.rating)
    FROM Review r
    WHERE r.call.translator.id = :translatorId
    """)
    Double findAverageRatingByTranslatorId(Long translatorId);

}
