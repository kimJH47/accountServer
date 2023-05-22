package core.accountserver.exception.transaction;

public class TransactionHasLockException extends RuntimeException {

	public TransactionHasLockException(String message) {
		super(message);
	}
}
