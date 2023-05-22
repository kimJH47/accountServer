package core.accountserver.exception.account;

public class AccountExceedBalanceException extends RuntimeException {
	public AccountExceedBalanceException(String message) {
		super(message);
	}
}
