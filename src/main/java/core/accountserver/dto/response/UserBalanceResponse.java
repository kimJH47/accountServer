package core.accountserver.dto.response;

import java.time.LocalDateTime;

import core.accountserver.domain.transaction.TransactionResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserBalanceResponse {
	private String accountNumber;
	private TransactionResultType transactionResultType;
	private LocalDateTime registeredAt;
}
