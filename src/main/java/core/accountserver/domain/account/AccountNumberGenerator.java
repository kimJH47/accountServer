package core.accountserver.domain.account;

public interface AccountNumberGenerator {

	String generator(Long userId);
}
