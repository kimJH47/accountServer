package core.accountserver.dto.response.transaction;

import java.time.LocalDateTime;

import core.accountserver.domain.transaction.TransactionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CancelBalanceResponse {
	private String accountNumber;
	private TransactionResult transactionResult;
	private String transactionId;
	private Long amount;
	private LocalDateTime transactedAt;
}
