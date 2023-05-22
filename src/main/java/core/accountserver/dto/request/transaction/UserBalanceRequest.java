package core.accountserver.dto.request.transaction;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserBalanceRequest {

	@NotNull(message = "아이디 값은 필수로 존재해야 합니다.")
	@Min(value = 1, message = "아이디는 1 이상 이여야 합니다.")
	private final Long userId;
	@NotNull(message = "계좌번호는 필수로 존재해야 합니다.")
	@Length(min = 10, max = 10, message = "계좌번호는 10자리여야합니다.")
	private final String accountNumber;
	@NotNull
	@Min(value = 10, message = "사용 최소금액은 10 입니다.")
	@Max(value = 1000_000_000, message = "사용 최대 금액은 1,000,000,000 입니다.")
	private Long amount;
}
