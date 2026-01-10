package com.paypal.transaction_service.transaction.repo;

import com.paypal.transaction_service.transaction.Transaction;

import java.util.List;

public interface TransactionRepositoryCustom {
    List<Transaction> findRecentByAccountId(Long accountId, int limit);
}

