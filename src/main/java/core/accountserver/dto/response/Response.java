package core.accountserver.dto.response;

import org.springframework.http.ResponseEntity;

import lombok.Getter;

@Getter
public class Response {
	String message;

	protected Response(String message) {
		this.message = message;
	}

	public static <T> ResponseEntity<Response> createSuccessResponse(String message, T entity) {
		return ResponseEntity.ok()
			.body(new SuccessResponse<>(message, entity));
	}
}
