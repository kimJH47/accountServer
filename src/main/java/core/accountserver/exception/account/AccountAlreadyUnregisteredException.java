package core.accountserver.exception.account;

public class AccountAlreadyUnregisteredException extends RuntimeException {
	public AccountAlreadyUnregisteredException(String message) {
		super(message);
	}
}
