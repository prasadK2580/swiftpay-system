package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA for {@code transactions} table.
 * Services should depend on domain repository interfaces, not this one directly.
 */
public interface TransactionJpaRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.transactionId = :transactionId")
    Optional<PaymentTransaction> findByTransactionIdForUpdate(@Param("transactionId") String transactionId);

    List<PaymentTransaction> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
            Long senderId, Long receiverId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PaymentTransaction t
            SET t.status = :newStatus
            WHERE t.transactionId = :transactionId AND t.status = :pendingStatus
            """)
    int updateStatusIfPending(
            @Param("transactionId") String transactionId,
            @Param("newStatus") TransactionStatus newStatus,
            @Param("pendingStatus") TransactionStatus pendingStatus);
}
