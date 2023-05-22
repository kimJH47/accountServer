package core.accountserver.service;

import static core.accountserver.domain.account.AccountStatus.*;
import static core.accountserver.domain.transaction.TransactionType.*;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.account.Account;
import core.accountserver.domain.transaction.Transaction;
import core.accountserver.domain.transaction.TransactionType;
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
	public UseBalanceResponse useBalance(Long userId, String accountNumber, Long amount) {

		AccountUser accountUser = accountUserRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));

		validUseBalance(accountUser, account, amount);

		account.useBalance(amount);
		Transaction transaction = transactionRepository.save(
			Transaction.createSuccessTransaction(account, amount, USE));

		return UseBalanceResponse.builder()
			.accountNumber(accountNumber)
			.transactionResult(transaction.getTransactionResult())
			.transactionId(transaction.getTransactionId())
			.amount(amount)
			.transactedAt(transaction.getTransactedAt())
			.build();
	}

	private void validUseBalance(AccountUser user, Account account, Long amount) {

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
	public void saveFailedTransaction(String accountNumber, Long amount, TransactionType transactionType) {
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));
		transactionRepository.save(Transaction.createFailTransaction(account, amount, transactionType));
	}

	@Transactional
	public CancelBalanceResponse cancelBalance(String transactionId, String accountNumber, Long amount) {
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));
		validAccount(account);
		Transaction transaction = transactionRepository.findByTransactionId(transactionId)
			.orElseThrow(() -> new TransactionNotFoundException("해당 거래내역이 존재하지 않습니다."));
		validCancelBalance(account, transaction, amount);

		account.cancelBalance(amount);
		Transaction createTransaction = transactionRepository.save(
			Transaction.createSuccessTransaction(account, amount, CANCEL));

		return CancelBalanceResponse.builder()
			.transactedAt(createTransaction.getTransactedAt())
			.transactionId(createTransaction.getTransactionId())
			.transactionResult(createTransaction.getTransactionResult())
			.accountNumber(accountNumber)
			.amount(createTransaction.getAmount())
			.build();
	}

	private void validCancelBalance(Account account, Transaction transaction, Long amount) {
		if (!Objects.equals(account.getId(), transaction.getAccount().getId())) {
			throw new AccountTransactionUnMatchException("해당계좌에서 발생된 거래가 아닙니다.");
		}
		if (transaction.isCancel()) {
			throw new TransactionAlreadyCancelException("이미 취소된 거래입니다.");
		}
		if (transaction.isFailed()) {
			throw new TransactionResultFailedException("해당 거래는 실패한 거래입니다.");
		}
		if (transaction.isValidTransactionDate()) {
			throw new TooOldOrderToCancelException("취소 가능한 거래 날짜가 지났습니다.");
		}
		if (!transaction.isSameAmount(amount)) {
			throw new CancelMustFullyException("취소금액은 거래된 금액과 일치 해야 합니다.");
		}
	}

	private void validAccount(Account account) {
		if (account.isUnRegistered()) {
			throw new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다.");
		}
	}
}
