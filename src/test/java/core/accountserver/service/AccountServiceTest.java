package core.accountserver.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import core.accountserver.dto.response.account.AccountSearchResponse;
import core.accountserver.dto.response.account.CreateAccountResponse;
import core.accountserver.dto.response.account.DeleteAccountResponse;
import core.accountserver.exception.account.AccountAlreadyUnregisteredException;
import core.accountserver.exception.account.AccountHasBalanceException;
import core.accountserver.exception.account.AccountNotFoundException;
import core.accountserver.exception.account.UserAccountUnMatchException;
import core.accountserver.exception.user.MaxAccountPerUserException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.generator.AccountNumberGenerator;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	AccountRepository accountRepository;
	@Mock
	AccountUserRepository accountUserRepository;

	FixedAccountNumberGenerator fixedAccountNumberGenerator = new FixedAccountNumberGenerator();
	AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(accountRepository, accountUserRepository, fixedAccountNumberGenerator);
	}

	@Test
	@DisplayName("user Id 와 초기금액을 받아서 account 생성 후 response 를 반환해야한다.")
	void create() {
		//given
		long userId = 1L;
		long initialBalance = 2000L;
		AccountUser accountUser = createAccountUser(userId, "user1");
		Account account = Account.create(accountUser, fixedAccountNumberGenerator.generator(userId), initialBalance,
			AccountStatus.IN_USE);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
		given(accountRepository.existsByAccountNumber(anyString())).willReturn(false);
		given(accountRepository.save(any(Account.class))).willReturn(account);

		//when
		CreateAccountResponse actual = accountService.createAccount(userId, initialBalance);
		//then
		assertThat(actual.getAccountNumber()).isEqualTo("1111111111");
		assertThat(actual.getUserId()).isEqualTo(userId);

		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).existsByAccountNumber(anyString());
		then(accountRepository).should(times(1)).save(any(Account.class));

	}

	@Test
	@DisplayName("계좌생성시 user id 가 존재하지 않으면 UserNotFoundException 이 던져진다.")
	void create_exception() {
		//given
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
		//expect
		assertThatThrownBy(() -> accountService.createAccount(1L, 2000L))
			.isInstanceOf(UserNotFoundException.class)
			.hasMessage("해당 사용자가 존재하지 않습니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
	}

	@Test
	@DisplayName("계좌 생성시 계좌가 10개를 초과 할 때 MaxAccountPerUserException 이 던져진다.")
	void create_max_account() {
		//given
		long userId = 1L;
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(createAccountUser(userId, "user")));
		given(accountRepository.countByAccountUser(any(AccountUser.class))).willReturn(10);

		//expect
		assertThatThrownBy(() -> accountService.createAccount(userId, 1000L))
			.isInstanceOf(MaxAccountPerUserException.class)
			.hasMessage("계좌가 이미 최대 갯수만큼 존재합니다.");

	}

	@Test
	@DisplayName("userId 와 accountNumber 를 받아서 계좌해지 후 response 를 반환해야한다.")
	void delete() {
		//given

		long userId = 1L;
		long initialBalance = 0L;
		AccountUser accountUser = createAccountUser(userId, "user1");
		Account account = Account.create(accountUser, fixedAccountNumberGenerator.generator(userId), initialBalance,
			AccountStatus.IN_USE);

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
		given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));

		//when
		DeleteAccountResponse actual = accountService.deleteAccount(userId, "1111111111");
		//then
		assertThat(actual.getAccountNumber()).isEqualTo("1111111111");
		assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.UNREGISTERED);

		then(accountRepository).should(times(0)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());

	}

	@Test
	@DisplayName("계좌해지 시 유저가 존재하지 않으면 UserNotFoundException 예외를 던져야한다.")
	void delete_userNotFound() {
		//given
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
		//expect
		assertThatThrownBy(() -> accountService.deleteAccount(1L, "1111111111"))
			.isInstanceOf(UserNotFoundException.class)
			.hasMessage("해당 사용자가 존재하지 않습니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
	}

	@Test
	@DisplayName("계좌해지 시 계좌가 존재하지 않으면 AccountNotFoundException 예외를 던져야한다.")
	void delete_accountNotFound() {
		//given
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(createAccountUser(1L, "user")));
		given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());
		//expect
		assertThatThrownBy(() -> accountService.deleteAccount(1L, "1234567890"))
			.isInstanceOf(AccountNotFoundException.class)
			.hasMessage("해당 계좌가 존재하지 않습니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
	}

	@Test
	@DisplayName("계좌에 잔액이 존재하면 AccountHasBalanceException 예외를 던져야한다.")
	void delete_hasBalance() {
		AccountUser user = createAccountUser(1L, "user");
		Account account = Account.create(user, "1234567890", 1000,
			AccountStatus.IN_USE);
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));
		//expect
		assertThatThrownBy(() -> accountService.deleteAccount(1L, "1234567890"))
			.isInstanceOf(AccountHasBalanceException.class)
			.hasMessage("해지하려는 계좌에 잔액이 존재합니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
	}

	@Test
	@DisplayName("사용자와 계좌 소유주가 다를 때 UserAccountUnMatchException 를 던져야한다.")
	void delete_unMatch() {
		AccountUser user = createAccountUser(1L, "user");
		Account account = Account.create(createAccountUser(2L, "user2"), "1234567890", 0,
			AccountStatus.IN_USE);
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));
		//expect
		assertThatThrownBy(() -> accountService.deleteAccount(1L, "1234567890"))
			.isInstanceOf(UserAccountUnMatchException.class)
			.hasMessage("사용자와 계좌의 소유주가 다릅니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
	}

	@Test
	@DisplayName("이미 해지된 계좌일 때  AccountAlreadyUnregisteredException 를 던져야한다.")
	void delete_alreadyUnregistered() {
		AccountUser user = createAccountUser(1L, "user");
		Account account = Account.create(user, "1234567890", 0L, AccountStatus.UNREGISTERED);
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));
		//expect
		assertThatThrownBy(() -> accountService.deleteAccount(1L, "1234567890"))
			.isInstanceOf(AccountAlreadyUnregisteredException.class)
			.hasMessage("이미 해지된 계좌번호 입니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountNumber(anyString());
	}

	@Test
	@DisplayName("userId 를 받아 해당하는 계좌번호 response 를 반환해야한다.")
	void find() {

		//given
		AccountUser user = createAccountUser(1L, "user");
		ArrayList<Account> accounts = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		accounts.add(new Account(1L, user, "1111111111", AccountStatus.IN_USE, 1000L, now, null));
		accounts.add(new Account(2L, user, "1111111112", AccountStatus.IN_USE, 2000L, now, null));
		accounts.add(new Account(3L, user, "1111111113", AccountStatus.IN_USE, 3000L, now, null));

		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
		given(accountRepository.findByAccountUser(any(AccountUser.class))).willReturn(accounts);

		//when
		List<AccountSearchResponse> actual = accountService.findAccountByUserId(1L);
		//then
		for (int i = 0; i < actual.size(); i++) {
			AccountSearchResponse expected = actual.get(i);
			assertThat(expected.getAccountNumber()).isEqualTo(accounts.get(i).getAccountNumber());
			assertThat(expected.getBalance()).isEqualTo(accounts.get(i).getBalance());
		}

	}

	@Test
	@DisplayName("userId 가 존재하지 않을 시 UserNotFoundException 을 던져야한다.")
	void find_userNotFound() {
		//given
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
		//expect
		assertThatThrownBy(() -> accountService.findAccountByUserId(1L))
			.isInstanceOf(UserNotFoundException.class)
			.hasMessage("해당 사용자가 존재하지 않습니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
	}

	@Test
	@DisplayName("계좌가 가 존재하지 않을 시 AccountNotFoundException 을 던져야한다.")
	void find_accountNotFound() {
		//given
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(createAccountUser(1L, "kim")));
		given(accountRepository.findByAccountUser(any(AccountUser.class))).willReturn(Collections.emptyList());
		//expect
		assertThatThrownBy(() -> accountService.findAccountByUserId(1L))
			.isInstanceOf(AccountNotFoundException.class)
			.hasMessage("해당 계좌가 존재하지 않습니다.");

		then(accountUserRepository).should(times(1)).findById(anyLong());
		then(accountRepository).should(times(1)).findByAccountUser(any(AccountUser.class));

	}

	private AccountUser createAccountUser(long userId, String name) {
		return new AccountUser(userId, name);
	}

	static class FixedAccountNumberGenerator implements AccountNumberGenerator {

		@Override
		public String generator(Long userId) {
			return "1111111111";
		}
	}
}