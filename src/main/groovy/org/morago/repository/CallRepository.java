package org.morago.repository;

import jakarta.persistence.LockModeType;
import org.morago.model.Call;
import org.morago.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CallRepository extends JpaRepository<Call, Long> {

    Page<Call> findByClient(User client, Pageable pageable);

    Page<Call> findByTranslator_User(User translator, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Call c WHERE c.id = :id")
    Optional<Call> findByIdForUpdate(Long id);

}
