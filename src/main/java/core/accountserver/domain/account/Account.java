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
}
