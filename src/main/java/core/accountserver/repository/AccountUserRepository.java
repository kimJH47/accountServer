package core.accountserver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;

@Repository
public interface AccountUserRepository extends JpaRepository<AccountUser,Long> {

	List<Account> findByAccountUser(AccountUser accountUser);
}
