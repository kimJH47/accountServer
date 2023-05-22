package core.accountserver.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import core.accountserver.dto.response.Response;
import core.accountserver.exception.user.MaxAccountPerUserException;
import core.accountserver.exception.user.UserNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {
	private static final String BAD_REQUEST = "잘못된 요청입니다.";
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Response> handle(MethodArgumentNotValidException e) {
		return Response.createBadRequestResponse(BAD_REQUEST, e);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Response> handle(UserNotFoundException e) {
		return Response.createBadRequestResponse(BAD_REQUEST, "userId", e.getMessage());
	}

	@ExceptionHandler(MaxAccountPerUserException.class)
	public ResponseEntity<Response> handle(MaxAccountPerUserException e) {
		return Response.createBadRequestResponse(BAD_REQUEST, "account", e.getMessage());
	}
}
