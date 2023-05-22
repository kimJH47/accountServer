package core.accountserver.service;

import static core.accountserver.domain.account.AccountStatus.*;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;
import core.accountserver.domain.transaction.Transaction;
import core.accountserver.dto.response.transaction.UserBalanceResponse;
import core.accountserver.exception.AccountAlreadyUnregisteredException;
import core.accountserver.exception.AccountExceedBalanceException;
import core.accountserver.exception.AccountNotFoundException;
import core.accountserver.exception.UserAccountUnMatchException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;
import core.accountserver.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final AccountUserRepository accountUserRepository;
	private final AccountRepository accountRepository;

	@Transactional
	public UserBalanceResponse useBalance(Long userId, String accountNumber, Long amount) {

		AccountUser accountUser = accountUserRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));

		validUserBalance(accountUser, account, amount);

		account.userBalance(amount);
		Transaction transaction = transactionRepository.save(Transaction.createSuccessTransaction(account, amount));

		return UserBalanceResponse.builder()
			.accountNumber(accountNumber)
			.transactionResult(transaction.getTransactionResultType())
			.transactionId(transaction.getTransactionId())
			.amount(amount)
			.transactedAt(transaction.getTransactedAt())
			.build();
	}

	private void validUserBalance(AccountUser user, Account account, Long amount) {

		if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
			throw new UserAccountUnMatchException("사용자와 계좌의 소유주가 다릅니다.");
		}
		if (account.getAccountStatus().equals(UNREGISTERED)) {
			throw new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다.");
		}
		if (account.getBalance() < amount) {
			throw new AccountExceedBalanceException("거래금액이 계좌 잔액보다 큽니다.");
		}

	}

	@Transactional
	public void saveFailedTransaction(String accountNumber, Long amount) {
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));
		transactionRepository.save(Transaction.createFailTransaction(account, amount));
	}
}
