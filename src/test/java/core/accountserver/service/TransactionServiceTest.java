package core.accountserver.service;

import static core.accountserver.domain.transaction.TransactionResult.*;
import static core.accountserver.domain.transaction.TransactionType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;
import core.accountserver.domain.account.AccountStatus;
import core.accountserver.domain.transaction.Transaction;
import core.accountserver.dto.response.transaction.UseBalanceResponse;
import core.accountserver.exception.account.AccountAlreadyUnregisteredException;
import core.accountserver.exception.account.AccountExceedBalanceException;
import core.accountserver.exception.account.AccountNotFoundException;
import core.accountserver.exception.account.UserAccountUnMatchException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;
import core.accountserver.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

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
	@DisplayName("계좌 사용 후 사용내역 response 가 반환되어야한다.")
	void useBalance() {
		//given
		long userId = 10L;
		String accountNumber = "1000000001";
		AccountUser user = createAccountUser(userId, "kim");
		Account account = createAccount(user, accountNumber, 1000L, AccountStatus.IN_USE);
		Transaction successTransaction = Transaction.createSuccessTransaction(account, 100L, USE);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.save(any(Transaction.class))).willReturn(successTransaction);

		//when
		UseBalanceResponse actual = transactionService.useBalance(userId, accountNumber, 100L);

		//then
		assertThat(actual.getTransactedAt()).isEqualTo(successTransaction.getTransactedAt().toString());
		assertThat(actual.getTransactionId()).isEqualTo(successTransaction.getTransactionId());
		assertThat(actual.getTransactionResult()).isEqualTo(SUCCESS);
		assertThat(actual.getAmount()).isEqualTo(100L);

		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).save(any(Transaction.class));
	}

	@Test
	@DisplayName("사용자가 존재하지 않으면 UserNotFoundException 이 던져 되어야한다.")
	void useBalance_userNotFound() {
		//given
		long userId = 10L;
		String accountNumber = "1000000001";
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

		//expect
		assertThatThrownBy(() -> transactionService.useBalance(userId, accountNumber, 100L))
			.isInstanceOf(UserNotFoundException.class);
		then(accountUserRepository).should(times(1)).findById(anyLong());
	}

	@Test
	@DisplayName("계좌가 존재하지 않으면 AccountNotFoundException 이 던져저야된다.")
	void useBalance_accountNotFound() {
		//given
		String accountNumber = "1000000001";
		AccountUser user = createAccountUser(1L, "kim");

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.empty());

		//expect
		assertThatThrownBy(() -> transactionService.useBalance(1L, accountNumber, 100L))
			.isInstanceOf(AccountNotFoundException.class);
		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());

	}

	@Test
	@DisplayName("사용자와 계좌 소유주가 다르면 UserAccountUnMatchException 이 던져저야한다.")
	void useBalance_userAccountUnMatch() {
		//given
		long userId = 10L;
		String accountNumber = "1000000001";
		AccountUser user = createAccountUser(userId, "kim");
		Account account = createAccount(createAccountUser(11L, "user"), accountNumber, 1000L, AccountStatus.IN_USE);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));

		//expect
		assertThatThrownBy(() -> transactionService.useBalance(1L, accountNumber, 100L))
			.isInstanceOf(UserAccountUnMatchException.class);

		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());

	}

	@Test
	@DisplayName("계좌가 해지상태면 AccountAlreadyUnregisteredException 이 던져저야한다.")
	void useBalance_accountAlreadyUnregistered() {
		//given
		long userId = 10L;
		String accountNumber = "1000000001";
		AccountUser user = createAccountUser(userId, "kim");
		Account account = createAccount(user, accountNumber, 1000L, AccountStatus.UNREGISTERED);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));

		//expect
		assertThatThrownBy(() -> transactionService.useBalance(userId, accountNumber, 999L))
			.isInstanceOf(AccountAlreadyUnregisteredException.class);

		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
	}

	@Test
	@DisplayName("사용금액이 현재 계좌의 금액보다 많으면 AccountExceedBalanceException 이 던져져야한다.")
	void useBalance_accountExceedBalance() {
		//given
		long userId = 10L;
		String accountNumber = "1000000001";
		AccountUser user = createAccountUser(userId, "kim");
		Account account = createAccount(user, accountNumber, 1500L, AccountStatus.IN_USE);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));

		//expect
		assertThatThrownBy(() -> transactionService.useBalance(userId, accountNumber, 1501L))
			.isInstanceOf(AccountExceedBalanceException.class);

		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
	}

	private AccountUser createAccountUser(long userId, String name) {
		return new AccountUser(userId, name);
	}

	private Account createAccount(
		AccountUser accountUser, String accountNumber, long balance, AccountStatus accountStatus) {
		return Account.create(accountUser, accountNumber, balance, accountStatus);
	}
}