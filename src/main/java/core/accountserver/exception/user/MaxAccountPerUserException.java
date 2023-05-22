package core.accountserver.exception.user;

public class MaxAccountPerUserException extends RuntimeException {

	public MaxAccountPerUserException(String message) {
		super(message);
	}
}
