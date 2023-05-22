package core.accountserver.exception;

public class AccountAlreadyUnregisteredException extends RuntimeException {
	public AccountAlreadyUnregisteredException(String message) {
		super(message);
	}
}
