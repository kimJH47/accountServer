package core.accountserver.exception.transaction;

public class TransactionFailedException extends RuntimeException {
	public TransactionFailedException(String message) {
		super(message);
	}
}
