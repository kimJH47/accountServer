package core.accountserver.dto.response;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

	public static ResponseEntity<Response> createBadRequestResponse(String message, MethodArgumentNotValidException e) {
		FailedResponse failedResponse = new FailedResponse(message);
		for (FieldError fieldError : e.getFieldErrors()) {
			failedResponse.input(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
			.body(failedResponse);
	}

	public static ResponseEntity<Response> createBadRequestResponse(String message,String fieldName,String reasons) {
		FailedResponse failedResponse = new FailedResponse(message);
		failedResponse.input(fieldName,reasons);
		return ResponseEntity.badRequest()
			.body(failedResponse);
	}
}
