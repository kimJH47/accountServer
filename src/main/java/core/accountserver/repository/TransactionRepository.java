package core.accountserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import core.accountserver.domain.transaction.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
	Optional<Transaction> findByTransactionId(String transactionId);

}
