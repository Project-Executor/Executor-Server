package com.seyoung5309.executorserver.domain.auth.repository;

import com.seyoung5309.executorserver.domain.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByEmail(String email);

    Optional<Account> findByEmail(String email);
}
