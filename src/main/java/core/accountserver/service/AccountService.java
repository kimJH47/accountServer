package core.accountserver.service;

import static core.accountserver.domain.account.AccountStatus.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;
import core.accountserver.generator.AccountNumberGenerator;
import core.accountserver.dto.response.CreateAccountResponse;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;
	private final AccountUserRepository accountUserRepository;

	private final AccountNumberGenerator accountNumberGenerator;

	@Transactional
	public CreateAccountResponse createAccount(Long userId, Long initialBalance) {
		AccountUser accountUser = accountUserRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다."));

		String createAccount = accountNumberGenerator.generator(userId);
		while (accountRepository.existsByAccountNumber(createAccount)) {
			createAccount = accountNumberGenerator.generator(userId);
		}
		Account account = accountRepository.save(Account.create(accountUser, createAccount, initialBalance, IN_USE));
		return CreateAccountResponse.builder()
			.userId(accountUser.getId())
			.accountNumber(account.getAccountNumber())
			.registeredAt(account.getRegisterAt())
			.build();
	}
}
