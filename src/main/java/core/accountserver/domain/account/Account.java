package core.accountserver.domain.account;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import core.accountserver.domain.AccountUser;
import core.accountserver.domain.TimeStampedEntity;
import core.accountserver.exception.account.AccountExceedBalanceException;
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
public class Account extends TimeStampedEntity {
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private AccountUser accountUser;
	private String accountNumber;
	@Enumerated(EnumType.STRING)
	private AccountStatus accountStatus;
	private Long balance;
	private LocalDateTime registerAt;
	private LocalDateTime unRegisteredAt;

	public void unRegistered() {
		unRegisteredAt = LocalDateTime.now();
		accountStatus = AccountStatus.UNREGISTERED;
	}

	public static Account create(
		AccountUser accountUser, String accountNumber, long initialBalance, AccountStatus accountStatus) {
		return Account.builder()
			.accountNumber(accountNumber)
			.accountStatus(accountStatus)
			.accountUser(accountUser)
			.balance(initialBalance)
			.registerAt(LocalDateTime.now())
			.build();
	}

	public void userBalance(Long amount) {
		if (amount > balance) {
			throw new AccountExceedBalanceException("거래금액이 계좌 잔액보다 큽니다.");
		}
		balance-= amount;
	}
}
