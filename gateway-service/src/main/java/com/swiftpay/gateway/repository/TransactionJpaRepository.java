package com.swiftpay.gateway.repository;

import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.shared.domain.enums.TransactionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA for {@code transactions} table.
 * Used internally by {@link PaymentRepository}; services should use {@link PaymentRepository}.
 */
public interface TransactionJpaRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

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
