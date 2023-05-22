package core.accountserver.exception.transaction;

public class TransactionAlreadyCancelException extends RuntimeException {
	public TransactionAlreadyCancelException(String message) {
		super(message);
	}
}
