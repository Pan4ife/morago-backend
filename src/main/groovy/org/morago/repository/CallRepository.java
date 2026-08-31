package org.morago.repository;

import jakarta.persistence.LockModeType;
import org.morago.model.Call;
import org.morago.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallRepository extends JpaRepository<Call, Long> {

    List<Call> findByClient(User client);

    List<Call> findByTranslator_User(User translator);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Call c WHERE c.id = :id")
    Optional<Call> findByIdForUpdate(Long id);

    @Query("SELECT c FROM Call c WHERE c.status = 'IN_PROGRESS' AND c.startTime < :threshold")
    List<Call> findStaleInProgressCalls(@Param("threshold") LocalDateTime threshold);

}
