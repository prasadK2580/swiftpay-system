package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA for {@code accounts} table.
 * Services should depend on {@link AccountRepository}, not this interface directly.
 */
public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    Optional<Account> findByIdForUpdate(@Param("userId") Long userId);
}
