package core.accountserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import core.accountserver.domain.account.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account,Long> {
	Optional<Account> findFirstByOrderByIdDesc();
}
