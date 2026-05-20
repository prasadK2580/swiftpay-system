package com.swiftpay.gateway.repo;

import com.swiftpay.gateway.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.transactionId = :transactionId")
    Optional<PaymentTransaction> findByTransactionIdForUpdate(@Param("transactionId") String transactionId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<PaymentTransaction> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
            Long senderId, Long receiverId, Pageable pageable);
}
