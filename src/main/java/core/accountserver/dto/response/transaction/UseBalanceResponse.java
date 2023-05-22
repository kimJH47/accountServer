package core.accountserver.dto.response.transaction;

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
public class UseBalanceResponse {
	private String accountNumber;
	private TransactionResultType transactionResult;
	private String transactionId;
	private Long amount;
	private LocalDateTime transactedAt;
}
