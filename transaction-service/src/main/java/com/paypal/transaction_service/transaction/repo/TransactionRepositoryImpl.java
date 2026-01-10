package com.paypal.transaction_service.transaction.repo;

import com.paypal.transaction_service.transaction.Transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Transaction> findRecentByAccountId(Long accountId, int limit) {
        String jpql = "SELECT t FROM Transaction t WHERE t.senderAccountId = :aid OR t.receiverAccountId = :aid ORDER BY t.createdAt DESC";
        TypedQuery<Transaction> q = em.createQuery(jpql, Transaction.class);
        q.setParameter("aid", accountId);
        q.setMaxResults(limit);
        return q.getResultList();
    }
}

