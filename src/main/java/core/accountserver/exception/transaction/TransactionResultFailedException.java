package core.accountserver.exception.transaction;

public class TransactionResultFailedException extends RuntimeException {
	public TransactionResultFailedException(String message) {
		super(message);
	}
}
