package core.accountserver.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateAccountRequest {

	@NotNull(message = "아이디 값은 필수로 존재해야 합니다.")
	@Min(value = 1, message = "아이디는 1 이상 이여야 합니다.")
	private final Long userId;
	@NotNull(message = "초기금액은 필수로 존재해야 합니다.")
	@Min(value = 100, message = "금액은 최소 100 이상 존재해야 합니다.")
	private final Long initialBalance;
}
