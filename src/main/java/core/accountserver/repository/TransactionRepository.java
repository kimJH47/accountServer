package core.accountserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import core.accountserver.domain.transaction.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
}
