package com.paypal.transaction_service.transaction.repo;

import com.paypal.transaction_service.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, TransactionRepositoryCustom {
    List<Transaction> findBySenderAccountId(Long senderAccountId);
    List<Transaction> findByReceiverAccountId(Long receiverAccountId);
    List<Transaction> findBySenderAccountIdOrReceiverAccountId(Long senderId, Long receiverId);
}

