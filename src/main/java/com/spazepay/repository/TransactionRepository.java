package com.spazepay.repository;

import com.spazepay.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccount_IdOrderByTransactionDateDesc(Long accountId);
    Optional<Transaction> findByIdAndAccount_Id(Long transactionId, Long accountId);
}