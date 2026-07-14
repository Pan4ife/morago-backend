package org.morago.repository;

import org.morago.model.Call;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRepository extends JpaRepository<Call, Long> {
}
