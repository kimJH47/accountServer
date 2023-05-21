package core.accountserver.dto.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

	@NotNull(message = "아이디 값은 필수로 입력 되어야합니다.")
	private Long userId;
	@Positive(message = "초기금액은 0 이상이여야 합니다.")
	private Long initialBalance;
}
