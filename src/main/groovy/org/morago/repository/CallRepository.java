package org.morago.repository;

import org.morago.model.Call;
import org.morago.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallRepository extends JpaRepository<Call, Long> {

    List<Call> findByClient(User client);

    List<Call> findByTranslator_User(User translator);

}
