package core.accountserver.dto.response.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountSearchResponse {

	private String accountNumber;
	private long balance;

	public static AccountSearchResponse create(String accountNumber, long balance) {
		return AccountSearchResponse.builder()
			.accountNumber(accountNumber)
			.balance(balance)
			.build();
	}

}
