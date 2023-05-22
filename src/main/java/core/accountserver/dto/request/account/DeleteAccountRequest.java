package core.accountserver.dto.request.account;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteAccountRequest {

	@NotNull(message = "아이디 값은 필수로 존재해야 합니다.")
	@Min(value = 1, message = "아이디는 1 이상 이여야 합니다.")
	private final Long userId;
	@NotNull(message = "계좌번호는 필수로 존재해야 합니다.")
	@Length(min = 10, max = 10, message = "계좌번호는 10자리여야합니다.")
	private final String accountNumber;
}
