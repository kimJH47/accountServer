package core.accountserver.service;

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
import core.accountserver.generator.AccountNumberGenerator;
import core.accountserver.domain.account.AccountStatus;
import core.accountserver.dto.response.CreateAccountResponse;
import core.accountserver.exception.user.UserNotFoundException;
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
	void create(){
		//given
		long userId = 1L;
		long initialBalance = 2000L;
		AccountUser accountUser = new AccountUser(userId, "user1");
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
	@DisplayName("계좌생성시 user id 가 존재하지 않으면 UserNotFoundException 이 던져진다. ")
	void create_exception(){
	    //given
		given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
		//expect
		assertThatThrownBy(() -> accountService.createAccount(1L, 2000L))
			.isInstanceOf(UserNotFoundException.class)
			.hasMessage("해당 사용자가 존재하지 않습니다.");
		then(accountUserRepository).should(times(1)).findById(anyLong());
	}

	static class FixedAccountNumberGenerator implements AccountNumberGenerator {

		@Override
		public String generator(Long userId) {
			return "1111111111";
		}
	}
}