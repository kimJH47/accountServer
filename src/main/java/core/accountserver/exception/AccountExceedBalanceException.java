package core.accountserver.exception;

public class AccountExceedBalanceException extends RuntimeException {
	public AccountExceedBalanceException(String message) {
		super(message);
	}
}
