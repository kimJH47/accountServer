package core.accountserver.service;

import org.springframework.stereotype.Service;

import core.accountserver.dto.response.CreateAccountResponse;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;
	private final AccountUserRepository accountUserRepository;
	public CreateAccountResponse createAccount(Long userId, Long initialBalance) {
		return null;
	}
}
