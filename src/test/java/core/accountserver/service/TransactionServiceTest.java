package core.accountserver.service;

import static core.accountserver.domain.transaction.TransactionResult.*;
import static core.accountserver.domain.transaction.TransactionType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
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
import core.accountserver.dto.response.transaction.CancelBalanceResponse;
import core.accountserver.dto.response.transaction.UseBalanceResponse;
import core.accountserver.exception.account.AccountAlreadyUnregisteredException;
import core.accountserver.exception.account.AccountExceedBalanceException;
import core.accountserver.exception.account.AccountNotFoundException;
import core.accountserver.exception.account.UserAccountUnMatchException;
import core.accountserver.exception.transaction.AccountTransactionUnMatchException;
import core.accountserver.exception.transaction.CancelMustFullyException;
import core.accountserver.exception.transaction.TooOldOrderToCancelException;
import core.accountserver.exception.transaction.TransactionAlreadyCancelException;
import core.accountserver.exception.transaction.TransactionNotFoundException;
import core.accountserver.exception.transaction.TransactionResultFailedException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.policy.TransactionConstant;
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
		long balance = 1000L;
		long amount = 100L;
		AccountUser user = createAccountUser(userId, "kim");

		Account account = createAccount(user, accountNumber, balance, AccountStatus.IN_USE);
		Transaction successTransaction = Transaction.createSuccessTransaction(account, amount, USE);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.save(any(Transaction.class))).willReturn(successTransaction);

		//when
		UseBalanceResponse actual = transactionService.useBalance(userId, accountNumber, amount);

		//then
		assertThat(actual.getTransactedAt()).isEqualTo(successTransaction.getTransactedAt().toString());
		assertThat(actual.getTransactionId()).isEqualTo(successTransaction.getTransactionId());
		assertThat(actual.getTransactionResult()).isEqualTo(SUCCESS);
		assertThat(actual.getAmount()).isEqualTo(amount);
		assertThat(account.getBalance()).isEqualTo(balance - amount);

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

	@Test
	@DisplayName("거래 취소 후 취소내역이 response 로 반환 되어야한다.")
	void cancelTransaction() {
		//given
		String accountNumber = "1100111111";
		long amount = 100L;
		long balance = 1000L;

		AccountUser user = createAccountUser(1L, "kim");
		Account account = createAccount(user, accountNumber, balance, AccountStatus.IN_USE);
		Transaction successTransaction = Transaction.createSuccessTransaction(account, amount, USE);
		Transaction cancelTransaction = Transaction.createSuccessTransaction(account, amount, CANCEL);

		given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(successTransaction));
		given(transactionRepository.save(any(Transaction.class))).willReturn(cancelTransaction);

		//when
		CancelBalanceResponse actual = transactionService.cancelBalance(successTransaction.getTransactionId(),
			accountNumber, amount);

		//then
		assertThat(actual.getTransactionId()).isEqualTo(cancelTransaction.getTransactionId());
		assertThat(actual.getTransactionResult()).isEqualTo(SUCCESS);
		assertThat(actual.getAccountNumber()).isEqualTo(accountNumber);
		assertThat(actual.getAmount()).isEqualTo(amount);
		assertThat(account.getBalance()).isEqualTo(balance + amount);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).save(any(Transaction.class));
		then(transactionRepository).should(times(1)).save(any(Transaction.class));
	}

	@Test
	@DisplayName("계좌가 존재하지 않으면 AccountNotFoundException 이 던져저야된다.")
	void cancelTransaction_accountNotFound() {
		//given
		String accountNumber = "1000000001";
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.empty());

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(AccountNotFoundException.class);
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());

	}

	@Test
	@DisplayName("거래내역이 존재하지 않으면 TransactionNotFoundException 이 던져저야된다.")
	void cancelTransaction_transactionNotFound() {
		//given
		String accountNumber = "1112111111";
		AccountUser user = createAccountUser(1L, "kim");
		Account account = createAccount(user, accountNumber, 1000L, AccountStatus.IN_USE);
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.empty());

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(TransactionNotFoundException.class);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).findByTransactionId(anyString());
	}

	@Test
	@DisplayName("거래취소 시 해지된 계좌면 AccountAlreadyUnregisteredException 을 던져야한다.")
	void cancelTransaction_AccountAlreadyUnregistered() {
		String accountNumber = "1112111111";
		AccountUser user = createAccountUser(1L, "kim");
		Account account = createAccount(user, accountNumber, 1000L, AccountStatus.UNREGISTERED);
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(AccountAlreadyUnregisteredException.class);
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());

	}

	@Test
	@DisplayName("거래취소 시 이미 취소된 거래면 AlreadyCancelException 을 던져야한다.")
	void cancelTransaction_alreadyCancel() {
		String accountNumber = "1112111111";
		AccountUser user = createAccountUser(1L, "kim");
		Account account = createAccount(user, accountNumber, 1000L, AccountStatus.IN_USE);
		Transaction transaction = Transaction.createSuccessTransaction(account, 100L, CANCEL);
		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(TransactionAlreadyCancelException.class);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).findByTransactionId(anyString());
	}

	@Test
	@DisplayName("거래취소시 거래내역의 계좌번호와 요청받은 계좌번호가 일치하지 않으면 AccountTransactionUnMatchException 를 던져야한다.")
	void cancel_AccountTransactionUnMatch() {
		//given
		String accountNumber = "1112111111";
		AccountUser user = createAccountUser(1L, "kim");
		LocalDateTime now = LocalDateTime.now();
		Account account1 = new Account(1L, user, accountNumber, AccountStatus.IN_USE, 1000L, now, now);
		Account account2 = new Account(2L, user, "1231412051", AccountStatus.IN_USE, 1000L, now, now);

		Transaction transaction = Transaction.createSuccessTransaction(account2, 100L, USE);

		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account1));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(AccountTransactionUnMatchException.class);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).findByTransactionId(anyString());

	}

	@Test
	@DisplayName("거래취소시 실패한 거래면 TransactionResultFailedException 을 던져야한다.")
	void cancel_TransactionResultFailed() {
		//given
		String accountNumber = "1112111111";
		AccountUser user = createAccountUser(1L, "kim");
		LocalDateTime now = LocalDateTime.now();
		Account account = new Account(1L, user, accountNumber, AccountStatus.IN_USE, 1000L, now, now);

		Transaction transaction = Transaction.createFailTransaction(account, 100L, USE);

		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(TransactionResultFailedException.class);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).findByTransactionId(anyString());
	}

	@Test
	@DisplayName("거래내역이 최대 정책날짜에 해당하는 시간이 지나면 TooOldOrderToCancelException 을 던져야한다.")
	void cancel_TooOldOrderToCancel() {
		//given
		String accountNumber = "1112111311";
		AccountUser user = createAccountUser(1L, "kim");
		LocalDateTime now = LocalDateTime.now();
		Account account = new Account(1L, user, accountNumber, AccountStatus.IN_USE, 2000L, now, now);

		LocalDateTime localDateTime = LocalDateTime.now()
			.minusYears(TransactionConstant.MAX_TRANSACTION_CANCEL_YEARS_BOUND);

		Transaction transaction = new Transaction(
			15L, USE, SUCCESS, account, 1500L, 99999L, "transactionId", localDateTime);

		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 100L))
			.isInstanceOf(TooOldOrderToCancelException.class);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).findByTransactionId(anyString());
	}

	@Test
	@DisplayName("거래취소 시 취소하려는 금액보다 거래내역 금액이 작으면 CancelMustFullyException 을 던져야한다.")
	void cancel_CancelMustFully() {
		//given
		String accountNumber = "1112111111";
		AccountUser user = createAccountUser(1L, "kim");
		LocalDateTime now = LocalDateTime.now();
		Account account = new Account(1L, user, accountNumber, AccountStatus.IN_USE, 1000L, now, now);
		Transaction transaction = Transaction.createSuccessTransaction(account, 100L, USE);

		given(accountRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(account));
		given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));

		//expect
		assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", accountNumber, 101L))
			.isInstanceOf(CancelMustFullyException.class);

		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
		then(transactionRepository).should(times(1)).findByTransactionId(anyString());

	}

	private AccountUser createAccountUser(long userId, String name) {
		return new AccountUser(userId, name);
	}

	private Account createAccount(
		AccountUser accountUser, String accountNumber, long balance, AccountStatus accountStatus) {
		return Account.create(accountUser, accountNumber, balance, accountStatus);
	}
}