package core.accountserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account,Long> {
	boolean existsByAccountNumber(String accountNumber);
	Integer countByAccountUser(AccountUser accountUser);
	Optional<Account> findByAccountNumber(String accountNumber);
}
