package core.accountserver.service;

import static core.accountserver.domain.account.AccountStatus.*;
import static core.accountserver.policy.AccountConstant.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;
import core.accountserver.dto.response.AccountSearchResponse;
import core.accountserver.dto.response.CreateAccountResponse;
import core.accountserver.dto.response.DeleteAccountResponse;
import core.accountserver.exception.AccountAlreadyUnregisteredException;
import core.accountserver.exception.AccountHasBalanceException;
import core.accountserver.exception.AccountNotFoundException;
import core.accountserver.exception.UserAccountUnMatchException;
import core.accountserver.exception.user.MaxAccountPerUserException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.generator.AccountNumberGenerator;
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
		validAccountCount(accountUser);
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

	private void validAccountCount(AccountUser accountUser) {
		if (accountRepository.countByAccountUser(accountUser) >= MAX_ACCOUNT_COUNT) {
			throw new MaxAccountPerUserException("계좌가 이미 최대 갯수만큼 존재합니다.");
		}
	}

	@Transactional
	public DeleteAccountResponse deleteAccount(Long userId, String accountNumber) {
		AccountUser accountUser = accountUserRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));

		validDeleteAccount(accountUser, account);
		account.unRegistered();

		return DeleteAccountResponse.builder()
			.accountNumber(accountNumber)
			.unRegisteredAt(account.getUnRegisteredAt())
			.build();
	}

	private void validDeleteAccount(AccountUser accountUser, Account account) {
		if (!Objects.equals(account.getAccountUser().getId(), accountUser.getId())) {
			throw new UserAccountUnMatchException("사용자와 계좌의 소유주가 다릅니다.");
		}
		if (account.getAccountStatus().equals(UNREGISTERED)) {
			throw new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다.");
		}
		if (account.getBalance() > 0) {
			throw new AccountHasBalanceException("해지하려는 계좌에 잔액이 존재합니다.");
		}
	}

	@Transactional(readOnly = true)
	public List<AccountSearchResponse> findAccountByUserId(long userId) {
		AccountUser accountUser = accountUserRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
		List<Account> accounts = accountRepository.findByAccountUser(accountUser);
		if (accounts.isEmpty()) {
			throw new AccountNotFoundException("해당 계좌가 존재하지 않습니다.");
		}
		return accounts.stream()
			.map(account -> AccountSearchResponse.create(account.getAccountNumber(), account.getBalance()))
			.collect(Collectors.toList());
	}
}
