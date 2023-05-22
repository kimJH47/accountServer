package core.accountserver.domain.transaction;

import static core.accountserver.domain.transaction.TransactionResult.*;
import static core.accountserver.domain.transaction.TransactionType.*;
import static core.accountserver.policy.TransactionConstant.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import core.accountserver.domain.TimeStampedEntity;
import core.accountserver.domain.account.Account;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Transaction extends TimeStampedEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Enumerated(EnumType.STRING)
	private TransactionType transactionType;
	@Enumerated(EnumType.STRING)
	private TransactionResult transactionResult;
	@ManyToOne
	private Account account;
	private Long amount;
	private Long balanceSnapshot;
	private String transactionId;
	private LocalDateTime transactedAt;

	public boolean isValidTransactionDate() {
		return transactedAt.isBefore(LocalDateTime.now().minusYears(MAX_TRANSACTION_CANCEL_YEARS_BOUND));
	}

	public boolean isCancel() {
		return transactionType.equals(CANCEL);
	}

	public boolean isFailed() {
		return transactionResult.equals(FAIL);
	}

	public boolean isSameAmount(Long amount) {
		return Objects.equals(this.amount, amount);
	}

	public static Transaction createSuccessTransaction(Account account, Long amount, TransactionType transactionType) {
		return Transaction.builder()
			.transactionType(transactionType)
			.transactionResult(SUCCESS)
			.account(account)
			.amount(amount)
			.balanceSnapshot(account.getBalance())
			.transactionId(UUID.randomUUID().toString().replace("-", ""))
			.transactedAt(LocalDateTime.now())
			.build();
	}

	public static Transaction createFailTransaction(Account account, Long amount) {
		return Transaction.builder()
			.transactionType(CANCEL)
			.transactionResult(FAIL)
			.account(account)
			.amount(amount)
			.balanceSnapshot(account.getBalance())
			.transactionId(UUID.randomUUID().toString().replace("-", ""))
			.transactedAt(LocalDateTime.now())
			.build();
	}
}
