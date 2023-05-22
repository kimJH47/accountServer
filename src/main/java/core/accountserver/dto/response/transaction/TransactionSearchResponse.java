package core.accountserver.dto.response.transaction;

import java.time.LocalDateTime;

import core.accountserver.domain.transaction.TransactionResult;
import core.accountserver.domain.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionSearchResponse {
	private String accountNumber;
	private TransactionType transactionType;
	private TransactionResult transactionResult;
	private String transactionId;
	private Long amount;
	private LocalDateTime transactedAt;
}
