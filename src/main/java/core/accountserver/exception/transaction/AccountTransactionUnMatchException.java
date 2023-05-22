package core.accountserver.exception.transaction;

public class AccountTransactionUnMatchException extends RuntimeException {
	public AccountTransactionUnMatchException(String message) {
		super(message);
	}
}
