package core.accountserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import core.accountserver.domain.AccountUser;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;
import core.accountserver.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
	public static final long USE_AMOUNT = 200L;
	public static final long CANCEL_AMOUNT = 200L;
	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private AccountUserRepository accountUserRepository;

	private TransactionService transactionService;


	@BeforeEach
	void setUp() {
		transactionService = new TransactionService(transactionRepository, accountUserRepository, accountRepository);
	}

	@Test
	void successUseBalance() {

	}

	private AccountUser createAccountUser(long userId, String name) {
		return new AccountUser(userId, name);
	}
}