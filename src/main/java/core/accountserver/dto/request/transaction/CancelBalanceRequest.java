package core.accountserver.dto.request.transaction;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelBalanceRequest {

	@NotNull(message = "아이디 값은 필수로 존재해야 합니다.")
	private final String transactionId;
	@NotNull(message = "계좌번호는 필수로 존재해야 합니다.")
	@Length(min = 10, max = 10, message = "계좌번호는 10자리여야합니다.")
	private final String accountNumber;
	@NotNull
	@Min(value = 1, message = "취소 최소금액은 1 입니다.")
	private Long amount;
}
