package core.accountserver.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteAccountResponse {
	private Long userId;
	private String accountNumber;
	private LocalDateTime unRegisteredAt;
}
