package core.accountserver.exception;

public class RedisClientException extends RuntimeException {

	public RedisClientException(InterruptedException e) {
		super(e.getMessage(), e.getCause());
	}
}
